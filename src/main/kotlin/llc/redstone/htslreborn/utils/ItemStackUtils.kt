package llc.redstone.htslreborn.utils

import llc.redstone.htslreborn.HTSLReborn.MC
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.GameType

object ItemStackUtils {
    suspend fun ItemStack.giveItem(slot: Int) {
        val gameMode = MC.player
            ?.let { MC.gameMode?.playerMode } ?: throw IllegalStateException("Could not determine player's game mode")
        if (gameMode != GameType.CREATIVE) CommandUtils.runCommand("/gmc")

        val pkt = ServerboundSetCreativeModeSlotPacket(
            slot,
            this
        )
        ClientThread.send {
            MC.connection?.send(pkt)
                ?: throw IllegalStateException("Something went wrong while creating item ${item.getName(this)}")
        }

        when (gameMode) {
            GameType.SURVIVAL -> CommandUtils.runCommand("/gms")
            GameType.ADVENTURE -> CommandUtils.runCommand("/gma")
            else -> {}
        }
    }

    fun ItemStack.getProperty(key: String): String? {
        return this.get(DataComponents.LORE)
            ?.lines()
            ?.firstNotNullOfOrNull { line ->
                line.string.substringAfter("$key: ").takeIf { it != line.string }
            }
    }

    fun ItemStack.getProperties(color: Boolean): List<Pair<String, String>> {
        return loreLines(color).mapNotNull { line ->
            val parts = line.split(": ", limit = 2)
            if (parts.size == 2) parts[0].replaceFirst("&7", "") to parts[1].replaceFirst("&f", "") else null
        }
    }

    fun ItemStack.getCurrentValue(color: Boolean): String? {
        val lore = loreLines(color)
        val startIndex = lore.indexOfFirst { line -> line.startsWith("Current Value:") }
        if (startIndex == -1) return null
        return lore.subList(startIndex + 1, lore.size)
            .takeWhile { line -> !line.isEmpty() }
            .joinToString(" ")
    }

    fun ItemStack.getLoreLine(line: Int, color: Boolean): String {
        return this.loreLines(color)
            .getOrNull(line)
            ?: throw IllegalStateException("Lore index out of bounds")
    }

    fun ItemStack.getLoreLineMatches(color: Boolean, filter: (String) -> Boolean = { true }): String {
        return this.loreLines(color)
            .firstOrNull { line -> filter(line) }
            ?: throw IllegalStateException(
                "No lore lines for item $this passed filter, with lore: ${
                    this.loreLines(
                        color
                    )
                }"
            )
    }

    fun ItemStack.getLoreLineMatchesOrNull(color: Boolean, filter: (String) -> Boolean = { true }): String? {
        return this.loreLines(color)
            .firstOrNull { line -> filter(line) }
    }

    fun ItemStack.loreLines(color: Boolean): List<String> {
        val loreLines = this.get(DataComponents.LORE)?.lines()
            ?: return emptyList()
        return loreLines.map { TextUtils.convertTextToString(it, color)!! }
    }

    fun List<String>.getLines(vararg line: Int): String? {
        return runCatching { line.joinToString(" ") { get(it) } }.getOrNull()
    }
}