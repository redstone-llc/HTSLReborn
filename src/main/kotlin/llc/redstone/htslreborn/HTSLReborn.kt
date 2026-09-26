package llc.redstone.htslreborn


//? if <26.1 {
//?} else {
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
*///?}
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import llc.redstone.htslreborn.commands.HTSLCommand
import llc.redstone.htslreborn.config.HTSLConfig
import llc.redstone.htslreborn.overlay.DebugHud
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.ui.browser.FileExplorerHandler
import llc.redstone.htslreborn.ui.browser.FileHandler
import llc.redstone.htslreborn.utils.ToastUtils
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import org.javers.core.JaversBuilder
import org.javers.core.diff.ListCompareAlgorithm
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files


//? if >=26.2 {
/*val Minecraft.screen: net.minecraft.client.gui.screens.Screen?
    get() = this.gui.screen()
*///?}

object HTSLReborn : ClientModInitializer {
    const val MOD_ID = "htslreborn"
    val LOGGER: Logger = LoggerFactory.getLogger("HTSL Reborn")
    const val VERSION = /*$ mod_version*/ "0.2.3";
    const val MINECRAFT = /*$ minecraft*/ "1.21.11";
    internal val JAVERS = JaversBuilder.javers()
        .withListCompareAlgorithm(ListCompareAlgorithm.LEVENSHTEIN_DISTANCE)
        .build()

    val MC = Minecraft.getInstance();

    val SCOPE = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    override fun onInitializeClient() {
        LOGGER.info("Loaded HTSL Reborn v$VERSION for Minecraft $MINECRAFT.")

        ClientTickEvents.START_CLIENT_TICK.register {
            Queue.onTick()
        }

        var notifiedResumable = false

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            if (Queue.pause()) {
                LOGGER.info("Paused import due to disconnect")
                notifiedResumable = false
            }
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (!notifiedResumable && Queue.session?.canResume == true) {
                ToastUtils.resumableSession()
                notifiedResumable = true
            }
        }

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            HTSLCommand.register(dispatcher)
        }

        DebugHud.register()

        HTSLConfig.load()
        val htslDir = HTSLConfig.importsPath()
        runCatching { Files.createDirectories(htslDir) }
        FileHandler.baseDir = htslDir
        FileHandler.currentDir = htslDir

        FileHandler.refreshFiles()
        FileExplorerHandler.setWatchedDir(FileHandler.currentDir)
        LOGGER.info(FileHandler.filteredFiles.toString())

        FileExplorerHandler.init()
    }
}