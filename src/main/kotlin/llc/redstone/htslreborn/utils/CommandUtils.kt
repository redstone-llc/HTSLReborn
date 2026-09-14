package llc.redstone.htslreborn.utils

import net.minecraft.client.Minecraft

object CommandUtils {
    suspend fun runCommand(command: String) = ClientThread.send {
        Minecraft.getInstance().connection
            ?.sendCommand(command) ?: throw IllegalStateException("Unable to send command $command")
    }
}
