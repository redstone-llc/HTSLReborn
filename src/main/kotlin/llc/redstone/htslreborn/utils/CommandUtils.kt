package llc.redstone.htslreborn.utils

import com.mojang.brigadier.suggestion.Suggestions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.config.HTSLConfig
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket
import kotlin.time.Duration.Companion.milliseconds

object CommandUtils {
    suspend fun runCommand(command: String) = ClientThread.send {
        Minecraft.getInstance().connection
            ?.sendCommand(command) ?: throw IllegalStateException("Unable to send command $command")
    }

    internal var pending: CompletableDeferred<List<String>>? = null
    suspend fun getTabCompletions(baseCommand: String): List<String> {
        val partialCommand = buildString {
            append(if (baseCommand.startsWith('/')) baseCommand else "/$baseCommand")
            if (!endsWith(' ')) append(' ')
        }

        val deferred = CompletableDeferred<List<String>>()
        pending?.cancel()
        pending = deferred

        return try {
            MC.connection?.send(ServerboundCommandSuggestionPacket(1, partialCommand))
            withTimeout(HTSLConfig.data.commandTimeout.milliseconds) { deferred.await() }
        } finally {
            if (pending === deferred) pending = null
        }
    }

    internal fun handleSuggestions(suggestions: Suggestions) {
        pending?.let { current ->
            pending = null
            current.complete(suggestions.list.map { it.text })
        }
    }
}
