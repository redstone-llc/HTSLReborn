package llc.redstone.htslreborn.queue

import kotlinx.coroutines.delay
import llc.redstone.htslreborn.data.Action
import llc.redstone.htslreborn.data.Condition
import llc.redstone.htslreborn.queue.Status.Failure
import llc.redstone.htslreborn.queue.Status.Success
import llc.redstone.htslreborn.queue.exporter.ExportSession
import llc.redstone.htslreborn.queue.exporter.Exporter
import llc.redstone.htslreborn.queue.exporter.Exporter.actions
import llc.redstone.htslreborn.queue.exporter.Exporter.args
import llc.redstone.htslreborn.queue.exporter.Exporter.conditions
import llc.redstone.htslreborn.queue.importer.ImportSession
import llc.redstone.htslreborn.utils.ClientThread
import llc.redstone.htslreborn.utils.CommandUtils
import llc.redstone.htslreborn.utils.InputUtils
import llc.redstone.htslreborn.utils.ItemStackUtils.giveItem
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.PredicateUtils.ItemMatch.ItemExact
import llc.redstone.htslreborn.utils.PredicateUtils.ItemSelector
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration.Companion.milliseconds

sealed interface Operation {
    val costKey: String get() = this::class.simpleName!!

    /** Exact cost in ms when known; null means it's learned from observations. */
    fun fixedCost(): Long? = null

    /** False if the op can't be estimated while pending (e.g. unbounded loops). */
    fun estimable(): Boolean = true

    suspend fun execute(mc: Minecraft): Status {
        // Default implementation does nothing
        return Success
    }

    data class OpenMenu(val gui: NameMatch, val slot: Int? = null, var checkIfOpened: Boolean = false) : Operation {
        override val costKey get() = if (checkIfOpened) "OpenMenu.check" else "OpenMenu"

        override suspend fun execute(mc: Minecraft): Status {
            if (slot != null) MenuUtils.interactionClick(slot)

            return if (MenuUtils.onOpen(gui, checkIfOpened).also {
                    Queue.guiContext = if (it != null) gui.cacheKey else null
                } != null) {
                Success
            } else {
                Failure("Failed to open menu: ${gui.cacheKey}")
            }
        }
    }

    data class Click(val slot: Int, val button: Int = 0) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            MenuUtils.interactionClick(slot, button)
            return Success
        }
    }

    data class ClickItem(val item: ItemSelector, val button: Int = 0) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
                val slot = MenuUtils.findSlots(item, paginated = true).firstOrNull()
                MenuUtils.interactionClick(slot?.index ?: error("Item '$item' not found"))
                return Success
            } catch (e: Exception) {
                return Failure("Failed to click item: ${e.message}")
            }
        }
    }

    data class Input(val text: String) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            return if (InputUtils.doInput(text)) {
                Success
            } else {
                Failure("Failed to input text: $text")
            }
        }
    }

    data class Option(val option: String) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
                val slot = MenuUtils.findSlots(option, paginated = true).firstOrNull()
                MenuUtils.interactionClick(slot?.index ?: error("Option '$option' not found"))
                return Success
            } catch (e: Exception) {
                return Failure("Failed to select option: ${e.message}")
            }
        }
    }

    data class Chat(
        val text: String,
        val createFallback: String? = null,
        val command: Boolean = false,
    ) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            if (command) {
                CommandUtils.runCommand(text)
            } else {
                ClientThread.send {
                    Minecraft.getInstance().connection
                        ?.sendChat(text) ?: error("Failed to send chat message")
                }
            }
            return Success
        }
    }

    data class Item(
        val stack: ItemStack? = null,
        val clickSlot: Int? = null,
    ) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            if (stack != null) {
                val oldStack = mc.player?.inventory?.getItem(26)
                stack.giveItem(26)
                MenuUtils.clickPlayerSlot(26)
                oldStack?.giveItem(26)
            } else if (clickSlot != null) {
                MenuUtils.interactionClick(clickSlot)
            } else {
                return Failure("Item operation must have either stack or clickSlot defined")
            }
            return Success
        }
    }

    data class GotoManual(val name: String) : Operation {
        override fun fixedCost() = 0L
    }

    data class Wait(val timeMs: Long) : Operation {
        override fun fixedCost() = timeMs

        override suspend fun execute(mc: Minecraft): Status {
            delay(timeMs.milliseconds)
            return Success
        }
    }

    data object DeleteActions : Operation {
        override fun estimable() = false

        override suspend fun execute(mc: Minecraft): Status {
            if (MenuUtils.findSlots(MenuItems.NO_ACTIONS).firstOrNull() != null) {
                return Success
            }

            while (true) {
                if (MenuUtils.findSlots(MenuItems.NO_ACTIONS).firstOrNull() != null) break

                MenuUtils.interactionClick(10, 1)
                delay((50 + InputUtils.getClientPing()).milliseconds)
            }
            return Success
        }
    }

    data object NextPage : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            if (MenuUtils.nextPage()) {
                return Success
            } else {
                return Failure("No next page available")
            }
        }
    }

    data object ExportActions : Operation {
        override fun estimable() = false

        override suspend fun execute(mc: Minecraft): Status {
            Exporter.handleActions()
            return Success
        }
    }

    data object ExportConditions : Operation {
        override fun estimable() = false

        override suspend fun execute(mc: Minecraft): Status {
            Exporter.handleConditions()
            return Success
        }
    }

    data class CompileCondition(val clazz: KClass<out Condition>, val inverted: Boolean) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            val constructor = clazz.primaryConstructor
                ?: return Failure("No primary constructor found for condition class: ${clazz.simpleName}")
            conditions.add(
                if (args.size != constructor.parameters.size) {
                    clazz.constructors.firstOrNull { it.parameters.size == constructor.parameters.size }
                        ?.callBy(args)
                        ?: constructor.callBy(args)
                } else {
                    constructor.isAccessible = true
                    constructor.callBy(args)
                }.apply { this.inverted = this@CompileCondition.inverted }
            )
            args.clear()
            return Success
        }
    }

    data class CompileAction(val clazz: KClass<out Action>) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            val constructor = clazz.primaryConstructor
                ?: return Failure("No primary constructor found for action class: ${clazz.simpleName}")
            actions.add(
                if (args.size != constructor.parameters.size) {
                    clazz.constructors.firstOrNull { it.parameters.size == constructor.parameters.size }
                        ?.callBy(args)
                        ?: constructor.callBy(args)
                } else {
                    constructor.isAccessible = true
                    constructor.callBy(args)
                }
            )
            args.clear()
            return Success
        }
    }

    data class SimpleProperty(val param: KParameter, val prop: KProperty1<Action, *>, val colorValue: String): Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            try {
                Exporter.handleSimpleProperty(prop, colorValue).let { value ->
                    args[param] = value
                }
            } catch (e: Exception) {
                return Failure("Failed to handle simple property '${prop.name}': ${e.message}")
            }
            return Success
        }
    }

    data class LongProperty(val param: KParameter, val prop: KProperty1<Action, *>, val colorValue: String, val propertyIndex: Int): Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
                Exporter.handleLongProperty(prop, colorValue, propertyIndex).let { value ->
                    args[param] = value
                }
            } catch (e: Exception) {
                return Failure("Failed to handle long property '${prop.name}': ${e.message}")
            }
            return Success
        }
    }

    data class ItemProperty(val param: KParameter, val propertyIndex: Int): Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
                Exporter.handleItemProperty(propertyIndex).let { value ->
                    args[param] = value
                }
            } catch (e: Exception) {
                return Failure("Failed to handle item property at index $propertyIndex: ${e.message}")
            }
            return Success
        }
    }

    /** Export counterpart of [Checkpoint]: the top-level action about to be read. */
    data class ExportCheckpoint(val index: Int) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            ExportSession.record(index)
            return Success
        }
    }

    /** Marks the start of an action; resume always restarts from the last one passed. */
    data class Checkpoint(val container: Int, val path: List<Int>) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            ImportSession.record(this)
            return Success
        }
    }

    /** Records how many actions already exist so resume knows where "ours" start. */
    data object CountActions : Operation {
        override fun estimable() = false

        override suspend fun execute(mc: Minecraft): Status {
            ImportSession.baseCount = MenuUtils.countActions()
            MenuUtils.goToFirstPage()
            return Success
        }
    }

    data class GotoPage(val page: Int) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            MenuUtils.goToFirstPage()
            repeat(page) {
                if (!MenuUtils.nextPage()) return Failure("Page $page does not exist")
            }
            return Success
        }
    }

    /** Deletes trailing actions until the open container holds exactly [expected]. */
    data class TrimActions(val expected: Int) : Operation {
        override fun estimable() = false

        override suspend fun execute(mc: Minecraft): Status {
            var count = MenuUtils.countActions()
            while (count > expected) {
                val last = MenuUtils.actionSlotsOnPage().maxByOrNull { it.index }
                    ?: return Failure("No action to delete on last page")
                MenuUtils.interactionClick(last.index, 1)
                delay((100 + InputUtils.getClientPing()).milliseconds)
                val next = MenuUtils.countActions()
                if (next >= count) return Failure("Failed to delete action at slot ${last.index}")
                count = next
            }
            MenuUtils.goToFirstPage()
            return Success
        }
    }

    data class Callback(val run: suspend () -> Status) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            return run()
        }
    }


    object MenuItems {
        val NO_ACTIONS = ItemSelector(
            name = NameExact("No Actions!"),
            item = ItemExact(Items.BEDROCK)
        )
    }
}
