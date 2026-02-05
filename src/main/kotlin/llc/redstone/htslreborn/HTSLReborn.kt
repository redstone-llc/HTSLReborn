package llc.redstone.htslreborn

import llc.redstone.htslreborn.commands.HTSLCommand
import llc.redstone.htslreborn.config.HtslConfig
import llc.redstone.htslreborn.ui.FileExplorerHandler
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object HTSLReborn : ClientModInitializer {
    const val MOD_ID = "htslreborn"
    val LOGGER: Logger = LoggerFactory.getLogger("HTSL Reborn")
    const val VERSION = /*$ mod_version*/ "0.0.1"
    const val MINECRAFT = /*$ minecraft*/ "1.21.9"
    val CONFIG: HtslConfig = HtslConfig.createAndLoad()
    val MC: MinecraftClient
        get() = MinecraftClient.getInstance()

    var importing = false
    var importingFile = ""
    var exporting = false
    var exportingFile = ""

    override fun onInitializeClient() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Loaded HTSL Reborn v$VERSION for Minecraft $MINECRAFT.")

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            HTSLCommand.register(dispatcher)
        }

        FileExplorerHandler.init()
    }
}