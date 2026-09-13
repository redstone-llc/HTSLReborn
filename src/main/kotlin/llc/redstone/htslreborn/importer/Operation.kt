package llc.redstone.htslreborn.importer

import kotlinx.coroutines.delay
import llc.redstone.htslreborn.importer.Status.Failure
import llc.redstone.htslreborn.importer.Status.Success
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
import kotlin.time.Duration.Companion.milliseconds

sealed interface Operation {
    suspend fun execute(mc: Minecraft): Status {
        // Default implementation does nothing
        return Success
    }

    data class OpenMenu(val gui: NameMatch, val slot: Int? = null, val checkIfOpened: Boolean = true) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            if (slot != null) MenuUtils.packetClick(slot)

            if (MenuUtils.onOpen(gui, checkIfOpened).also {
                Queue.guiContext = if (it != null) gui.cacheKey else null
            } != null) {
                return Success
            } else {
                return Failure("Failed to open menu: ${gui.cacheKey}")
            }
        }
    }

    data class Click(val slot: Int, val button: Int = 0) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            MenuUtils.packetClick(slot, button)
            return Success
        }
    }

    data class ClickItem(val item: ItemSelector, val button: Int = 0) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
                val slot = MenuUtils.findSlots(item, paginated = true).firstOrNull()
                MenuUtils.packetClick(slot?.index ?: error("Item '$item' not found"))
                return Success
            } catch (e: Exception) {
                return Failure("Failed to click item: ${e.message}")
            }
        }
    }

    data class Input(val text: String) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            if (InputUtils.handleInput(text)) {
                return Success
            } else {
                return Failure("Failed to input text: $text")
            }
        }
    }

    data class Option(val option: String) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            try {
                val slot = MenuUtils.findSlots(option, paginated = true).firstOrNull()
                MenuUtils.packetClick(slot?.index ?: error("Option '$option' not found"))
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
                Minecraft.getInstance().connection
                    ?.sendChat(text) ?: error("Failed to send chat message")
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
                MenuUtils.packetClick(clickSlot)
            } else {
                return Failure("Item operation must have either stack or clickSlot defined")
            }
            return Success
        }
    }

    data class GotoManual(val name: String) : Operation

    data class Wait(val timeMs: Long) : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            delay(timeMs.milliseconds)
            return Success
        }
    }

    data object DeleteActions : Operation {
        override suspend fun execute(mc: Minecraft): Status {
            if (MenuUtils.findSlots(MenuItems.NO_ACTIONS).firstOrNull() != null) {
                return Success
            }

            while (true) {
                if (MenuUtils.findSlots(MenuItems.NO_ACTIONS).firstOrNull() != null) break

                MenuUtils.packetClick(10, 1)
                delay((50 + InputUtils.getClientPing()).milliseconds)
            }
            return Success
        }
    }

    data class SetGuiContext(val context: String) : Operation

    data class Callback(val run: () -> Unit) : Operation

    data object Done : Operation

    object MenuItems {
        val NO_ACTIONS = ItemSelector(
            name = NameExact("No Actions!"),
            item = ItemExact(Items.BEDROCK)
        )
    }
}
