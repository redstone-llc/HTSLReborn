package llc.redstone.htslreborn

import llc.redstone.htslreborn.commands.HTSLCommand
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.utils.RenderUtils.isInitialized
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.minecraft.client.MinecraftClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.FileSystems
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import kotlin.io.path.name

object HTSLReborn : ClientModInitializer {
    val MOD_ID = "htslreborn"
    val LOGGER: Logger = LoggerFactory.getLogger("HTSL Reborn")
    val VERSION = /*$ mod_version*/ "0.0.1"
    val MINECRAFT = /*$ minecraft*/ "1.21.9"
    val MC: MinecraftClient
        get() = MinecraftClient.getInstance()

    var importing = false
    var importingFile = ""

    val fileEditCooldown = mutableMapOf<String, Long>()

    override fun onInitializeClient() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        LOGGER.info("Loaded HTSL Reborn v$VERSION for Minecraft $MINECRAFT.");

        ClientCommandRegistrationCallback.EVENT.register { dispatcher, registryAccess ->
            HTSLCommand.register(dispatcher)
        }

        val watcher = FileSystems.getDefault().newWatchService()

        val dir = Paths.get("./HTSL/imports")
        dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        Thread {
            while (true) {
                val key = watcher.take()
                for (event in key.pollEvents()) {
                    val kind = event.kind()
                    val filename = event.context() as java.nio.file.Path
                    if (fileEditCooldown.containsKey(filename.name)) {
                        val lastEdit = fileEditCooldown[filename.name]!!
                        if (System.currentTimeMillis() - lastEdit < 100) {
                            continue
                        }
                    }
                    fileEditCooldown[filename.name] = System.currentTimeMillis()
                    if (FileExplorer.INSTANCE.isInitialized()) {
                        FileHandler.refreshFiles(true)
                        FileExplorer.INSTANCE.refreshExplorer(true)
                    }
                }
                val valid = key.reset()
                if (!valid) {
                    break
                }
            }
        }.start()
    }
}