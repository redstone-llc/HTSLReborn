package llc.redstone.htslreborn.importer

import kotlinx.coroutines.delay
import llc.redstone.htslreborn.utils.CommandUtils
import llc.redstone.htslreborn.utils.InputUtils
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.PredicateUtils
import llc.redstone.htslreborn.utils.PredicateUtils.ItemMatch.ItemExact
import llc.redstone.htslreborn.utils.PredicateUtils.ItemSelector
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameExact
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import kotlin.time.Duration.Companion.milliseconds

sealed interface Operation {
    suspend fun execute(mc: Minecraft): Boolean {
        // Default implementation does nothing
        return true
    }

    data class OpenMenu(val gui: NameMatch, val slot: Int? = null, val checkIfOpened: Boolean = true) : Operation {
        override suspend fun execute(mc: Minecraft): Boolean {
            if (slot != null) MenuUtils.packetClick(slot)

            return MenuUtils.onOpen(gui, checkIfOpened) != null
        }
    }

    data class Click(val slot: Int, val button: Int = 0) : Operation {
        override suspend fun execute(mc: Minecraft): Boolean {
            MenuUtils.packetClick(slot, button)
            return true
        }
    }

    data class Input(val text: String) : Operation {
        override suspend fun execute(mc: Minecraft): Boolean {
            return InputUtils.handleInput(text)
        }
    }

    data class Option(val option: String) : Operation

    data class Chat(
        val text: String,
        val createFallback: String? = null,
        val command: Boolean = false,
    ) : Operation {
        override suspend fun execute(mc: Minecraft): Boolean {
            if (command) {
                CommandUtils.runCommand(text)
            } else {
                Minecraft.getInstance().connection
                    ?.sendChat(text) ?: error("Failed to send chat message")
            }
            return true
        }
    }

    data class Item(
        val stack: ItemStack? = null,
        val clickSlot: Int? = null,
    ) : Operation

    data class GotoManual(val name: String) : Operation

    data class Wait(val timeMs: Long) : Operation {
        override suspend fun execute(mc: Minecraft): Boolean {
            delay(timeMs.milliseconds)
            return true
        }
    }

    data object DeleteActions : Operation {
        override suspend fun execute(mc: Minecraft): Boolean {
            if (MenuUtils.findSlots(MenuItems.NO_ACTIONS).firstOrNull() != null) {
                return true
            }

            while (true) {
                if (MenuUtils.findSlots(MenuItems.NO_ACTIONS).firstOrNull() != null) break

                MenuUtils.packetClick(10, 1)
                delay((50 + InputUtils.getClientPing()).milliseconds)
            }
            return true
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
