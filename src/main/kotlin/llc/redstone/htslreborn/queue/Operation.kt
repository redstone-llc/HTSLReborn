package llc.redstone.htslreborn.queue

import kotlinx.coroutines.delay
import llc.redstone.htslreborn.data.Action
import llc.redstone.htslreborn.data.Condition
import llc.redstone.htslreborn.queue.Status.Failure
import llc.redstone.htslreborn.queue.Status.Success
import llc.redstone.htslreborn.queue.differ.DiffSession
import llc.redstone.htslreborn.queue.exporter.ExportSession
import llc.redstone.htslreborn.queue.exporter.Exporter
import llc.redstone.htslreborn.queue.importer.ImportSession
import llc.redstone.htslreborn.utils.*
import llc.redstone.htslreborn.utils.ItemStackUtils.giveItem
import llc.redstone.htslreborn.utils.PredicateUtils.ItemMatch.ItemExact
import llc.redstone.htslreborn.utils.PredicateUtils.ItemSelector
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import net.minecraft.client.Minecraft
import net.minecraft.world.inventory.ContainerInput
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
                Failure(TextUtils.translate("htslreborn.error.open_menu", gui.cacheKey))
            }
        }
    }

    data class Click(val slot: Int, val button: Int = 0, val actionType: ContainerInput = ContainerInput.PICKUP) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            MenuUtils.interactionClick(slot, button, actionType)
            return Success
        }
    }

    data class ClickItem(val item: ItemSelector, val button: Int = 0) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
            val slot = MenuUtils.findSlots(item, paginated = true).firstOrNull()
            MenuUtils.interactionClick(slot?.index ?: error(TextUtils.translate("htslreborn.error.item_not_found", item)))
            return Success
            } catch (e: Exception) {
                return Failure(TextUtils.translate("htslreborn.error.click_item", e.message ?: ""))
            }
        }
    }

    data class Input(val text: String) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            return if (InputUtils.doInput(text)) {
                Success
            } else {
                Failure(TextUtils.translate("htslreborn.error.input_text", text))
            }
        }
    }

    data class Option(val option: String) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
                val slot = MenuUtils.findSlots(option, paginated = true).firstOrNull()
                MenuUtils.interactionClick(slot?.index ?: error(TextUtils.translate("htslreborn.error.option_not_found", option)))
                return Success
            } catch (e: Exception) {
                return Failure(TextUtils.translate("htslreborn.error.select_option", e.message ?: ""))
            }
        }
    }

    data class OpenOrCreate(
        val name: String,
        val openCommand: String,
        val createCommand: String,
        val menu: NameMatch,
    ): Operation {
        override suspend fun execute(mc: Minecraft): Status {
            val suggestions = CommandUtils.getTabCompletions(openCommand)
            if (suggestions.contains(name)) {
                CommandUtils.runCommand(openCommand + name)
                MenuUtils.onOpen(menu, checkIfOpened = false) ?: return Failure(TextUtils.translate("htslreborn.error.open_menu", menu.cacheKey))
            } else {
                CommandUtils.runCommand(createCommand)
                MenuUtils.onOpen(menu, checkIfOpened = false) ?: return Failure(TextUtils.translate("htslreborn.error.open_menu", menu.cacheKey))
            }
            return Success
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
                        ?.sendChat(text) ?: error(TextUtils.translate("htslreborn.error.chat_failed"))
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

    data class Wait(val timeMs: Long) : Operation {
        override fun fixedCost() = timeMs

        override suspend fun execute(mc: Minecraft): Status {
            delay(timeMs.milliseconds)
            return Success
        }
    }

    data object NextPage : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            if (MenuUtils.nextPage()) {
                return Success
            } else {
                return Failure(TextUtils.translate("htslreborn.error.no_next_page"))
            }
        }
    }

    data class ExportActions(val param: KParameter) : Operation {
        override fun estimable() = false

        override suspend fun execute(mc: Minecraft): Status {
            val collected = mutableListOf<Action>()
            Exporter.pushActionTarget(collected)
            val nested = try {
                Exporter.readAllPages(Exporter.MenuItems.NO_ACTIONS) { Exporter.handleActions() }
            } catch (e: Exception) {
                Exporter.popActionTarget()
                return Failure(TextUtils.translate("htslreborn.error.execute", this, e.message ?: ""))
            }
            nested.add(CommitActions(param))
            Queue.addAll(nested, 0)
            return Success
        }
    }

    data class ExportConditions(val param: KParameter) : Operation {
        override fun estimable() = false

        override suspend fun execute(mc: Minecraft): Status {
            val collected = mutableListOf<Condition>()
            Exporter.pushConditionTarget(collected)
            val nested = try {
                Exporter.readAllPages(Exporter.MenuItems.NO_CONDITIONS) { Exporter.handleConditions() }
            } catch (e: Exception) {
                Exporter.popConditionTarget()
                return Failure(TextUtils.translate("htslreborn.error.execute", this, e.message ?: ""))
            }
            nested.add(CommitConditions(param))
            Queue.addAll(nested, 0)
            return Success
        }
    }

    data class CommitActions(val param: KParameter) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            Exporter.currentArgs()[param] = Exporter.popActionTarget().toList()
            return Success
        }
    }

    data class CommitConditions(val param: KParameter) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            Exporter.currentArgs()[param] = Exporter.popConditionTarget().toList()
            return Success
        }
    }

    data object PushArgs : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            Exporter.pushArgs()
            return Success
        }
    }

    data class CompileCondition(val clazz: KClass<out Condition>, val inverted: Boolean) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            val frame = Exporter.popArgs()
            val constructor = clazz.primaryConstructor
            if (constructor == null) {
                Exporter.pushArgs(frame)
                return Failure("No primary constructor found for condition class: ${clazz.simpleName}")
            }
            val condition = try {
                if (frame.size != constructor.parameters.size) {
                    clazz.constructors.firstOrNull { it.parameters.size == constructor.parameters.size }
                        ?.callBy(frame)
                        ?: constructor.callBy(frame)
                } else {
                    constructor.isAccessible = true
                    constructor.callBy(frame)
                }
            } catch (e: Exception) {
                Exporter.pushArgs(frame)
                return Failure("Failed to compile condition ${clazz.simpleName}: ${e.message}")
            }
            condition.inverted = inverted
            Exporter.currentConditions().add(condition)
            return Success
        }
    }

    data class CompileAction(val clazz: KClass<out Action>) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            val frame = Exporter.popArgs()
            val constructor = clazz.primaryConstructor
            if (constructor == null) {
                Exporter.pushArgs(frame)
                return Failure("No primary constructor found for action class: ${clazz.simpleName}")
            }
            val action = try {
                if (frame.size != constructor.parameters.size) {
                    clazz.constructors.firstOrNull { it.parameters.size == constructor.parameters.size }
                        ?.callBy(frame)
                        ?: constructor.callBy(frame)
                } else {
                    constructor.isAccessible = true
                    constructor.callBy(frame)
                }
            } catch (e: Exception) {
                Exporter.pushArgs(frame)
                return Failure("Failed to compile action ${clazz.simpleName}: ${e.message}")
            }
            Exporter.currentActions().add(action)
            return Success
        }
    }

    data class SimpleProperty(val param: KParameter, val prop: KProperty1<Action, *>, val colorValue: String): Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            try {
                Exporter.handleSimpleProperty(prop, colorValue).let { value ->
                    Exporter.currentArgs()[param] = value
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
                    Exporter.currentArgs()[param] = value
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
                    Exporter.currentArgs()[param] = value
                }
            } catch (e: Exception) {
                return Failure("Failed to handle item property at index $propertyIndex: ${e.message}")
            }
            return Success
        }
    }

    /** Drops export state past [keep] so a scan (re)starting at that index appends cleanly. */
    data class ResetExport(val keep: Int) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            Exporter.clearNestedExport()
            while (Exporter.actions.size > keep) Exporter.actions.removeLast()
            ExportSession.begin()
            return Success
        }
    }

    /** Records which container and phase a diff is in, for [DiffSession] to resume from. */
    data class DiffPhase(val phase: DiffSession.Phase) : Operation {
        override fun fixedCost() = 0L

        override suspend fun execute(mc: Minecraft): Status {
            DiffSession.record(phase)
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
    data class Checkpoint(val path: List<Int>) : Operation {
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
            MenuUtils.gotoPage(page)
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
