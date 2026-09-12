package llc.redstone.htslreborn.importer

import llc.redstone.htslreborn.utils.CommandUtils
import llc.redstone.htslreborn.utils.MenuUtils
import llc.redstone.htslreborn.utils.PredicateUtils
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch
import llc.redstone.htslreborn.utils.PredicateUtils.NameMatch.NameContains
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.world.item.ItemStack

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

    data class ClickByName(
        val name: String,
        val fallbackSlot: Int,
        val button: Int = 0,
    ) : Operation

    data class Input(val text: String) : Operation

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

    data object Back : Operation

    data object ReturnToEditActions : Operation

    data object ReturnToActionSettings : Operation

    data object CloseGui : Operation

    data class GotoManual(val name: String) : Operation

    data class Wait(val timeMs: Long) : Operation

    data object DeleteActions : Operation

    data class SetGuiContext(val context: String) : Operation

    data class Callback(val run: () -> Unit) : Operation

    data object Done : Operation
}
