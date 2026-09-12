package llc.redstone.htslreborn.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import llc.redstone.htslreborn.HTSLReborn.MC
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import kotlin.time.Duration.Companion.milliseconds

object InputUtils {
    var pendingInput: CompletableDeferred<Type>? = null

    suspend fun handleInput(input: String): Boolean {
        pendingInput = CompletableDeferred()

        return try {
            withTimeout(5000.milliseconds) {
                val type = pendingInput?.await() ?: return@withTimeout false
                return@withTimeout when (type) {
                    Type.CHAT -> {
                        Minecraft.getInstance().connection
                            ?.sendChat(input) ?: error("Failed to send chat message")
                        true
                    }

                    Type.ANVIL -> {
                        val screen = MenuUtils.onOpen(PredicateUtils.NameMatch.NameContains("Value:")) as? AnvilScreen
                            ?: return@withTimeout false
                        delay((200 + getClientPing()).milliseconds)
                        if (screen.menu.setItemName(input)) {
                            MC.connection?.send(ServerboundRenameItemPacket(input))
                        }
                        MenuUtils.interactionClick(2)
                        true
                    }
                }
            }
        } catch (e: Exception) {
            false
        } finally {
            pendingInput = null
        }
    }

    fun handleInputType(type: Type) {
        pendingInput?.complete(type)
    }

    fun getClientPing(): Int {
        val playerInfo = MC.connection?.getPlayerInfo(MC.player?.uuid ?: return -1) ?: return -1
        return playerInfo.latency
    }

    enum class Type {
        CHAT,
        ANVIL
    }
}