package llc.redstone.htslreborn.utils

//? if >=26.2 {
/*import llc.redstone.htslreborn.screen
*///?}

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import llc.redstone.htslreborn.HTSLReborn.MC
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AnvilScreen
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object InputUtils {
    var pendingInput: CompletableDeferred<Type>? = null
    var type: Type? = null

    suspend fun doInput(input: String): Boolean {
        pendingInput = CompletableDeferred()

        return try {
            if (type != null) {
                return handleInput(input, type!!)
            }
            withTimeout(5000.milliseconds) {
                val type = pendingInput?.await() ?: return@withTimeout false
                return@withTimeout handleInput(input, type)
            }
        } catch (e: Exception) {
            false
        } finally {
            pendingInput = null
        }
    }

    suspend fun handleInput(input: String, type: Type): Boolean {
        try {
            return when (type) {
                Type.CHAT -> {
                    ClientThread.send {
                        Minecraft.getInstance().connection
                            ?.sendChat(input) ?: error("Failed to send chat message")
                    }
                    this.type = null
                    true
                }

                Type.ANVIL -> {
                    val screen = MC.screen as? AnvilScreen ?: return false
                    delay(200.milliseconds)

                    if (ClientThread.run { screen.menu.setItemName(input) }) {
                        ClientThread.send { MC.connection?.send(ServerboundRenameItemPacket(input)) }
                    }
                    MenuUtils.interactionClick(2)
                    awaitScreen("Failed to close anvil screen") { it !is AnvilScreen }
                    this.type = null
                    true
                }
            }
        } finally {
        }
    }

    private suspend fun awaitScreen(failure: String, timeout: Duration = 5000.milliseconds, predicate: (Screen?) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds
        while (!predicate(MC.screen)) {
            if (System.currentTimeMillis() > deadline) error(failure)
            delay(50.milliseconds)
        }
    }

    fun handleInputType(type: Type) {
        this.type = type
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