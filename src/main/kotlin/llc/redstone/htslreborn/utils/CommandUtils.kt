package llc.redstone.htslreborn.utils

import net.minecraft.client.Minecraft

object CommandUtils {
    fun runCommand(command: String) {
        Minecraft.getInstance().connection
            ?.sendCommand(command) ?: throw IllegalStateException("Unable to send command $command")

    }
}