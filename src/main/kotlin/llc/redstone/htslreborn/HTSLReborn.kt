package llc.redstone.htslreborn


//? if <26.1 {
//?} else {
/*import net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal
*///?}
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import llc.redstone.htslreborn.data.ContextTarget
import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.overlay.DebugHud
import llc.redstone.htslreborn.parser.ast.HtslAstBuilder
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.differ.Differ
import llc.redstone.htslreborn.queue.exporter.Exporter
import llc.redstone.htslreborn.queue.importer.Importer
import llc.redstone.htslreborn.ui.browser.FileExplorerHandler
import llc.redstone.htslreborn.ui.browser.FileHandler
import llc.redstone.htslreborn.utils.ToastUtils
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.Minecraft
import org.javers.core.JaversBuilder
import org.javers.core.diff.ListCompareAlgorithm
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Paths


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

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            if (Queue.pause()) LOGGER.info("Paused import due to disconnect")
        }

        var notifiedResumable = false
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (!notifiedResumable && Queue.session?.canResume == true) {
                ToastUtils.send(
                    "§aResumable session available",
                    "§7Use §e/htsl resume§7 to continue the last operation."
                )
                notifiedResumable = true
            }
        }

        DebugHud.register()

        val htslDir = MC.gameDirectory.toPath().resolve("htsl")
        FileHandler.baseDir = htslDir
        FileHandler.currentDir = htslDir

        FileHandler.refreshFiles()
        FileExplorerHandler.setWatchedDir(FileHandler.currentDir)
        LOGGER.info(FileHandler.filteredFiles.toString())

        FileExplorerHandler.init()

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, context ->
            dispatcher.register(
                literal("htsl")
                    .executes {
                        val home = Paths.get(System.getProperty("user.home"))
                        val htslFile = home.resolve("Desktop/htsl/test.htsl")
                        val ast = HtslAstBuilder.parseFile(htslFile)
                        Importer.process(ast, htslFile)
                        1
                    }
                    .then(literal("export").executes {
                        val home = Paths.get(System.getProperty("user.home"))
                        val outputFile = home.resolve("Desktop/htsl/output.htsl")
                        Exporter.process(ScriptContainer(ImportContext.FUNCTION, ContextTarget("test")), outputFile)
                        1
                    })
                    .then(literal("diff").executes {
                        val home = Paths.get(System.getProperty("user.home"))
                        val htslFile = home.resolve("Desktop/htsl/test.htsl")
                        val ast = HtslAstBuilder.parseFile(htslFile)
                        Differ.process(ast, htslFile)
                        1
                    })
                    .then(literal("clear").executes {
                        Queue.clear()
                        1
                    })
                    .then(literal("resume").executes {
                        runCatching { Queue.resume() }
                            .onSuccess { ToastUtils.send("§aResumed", "§7Continuing from the last action.") }
                            .onFailure { ToastUtils.send("§cCan't resume", "§7${it.message}") }
                        1
                    })
            )
        }

        runCatching { Files.createDirectories(htslDir) }
    }
}