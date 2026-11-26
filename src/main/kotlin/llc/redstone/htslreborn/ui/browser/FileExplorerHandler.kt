package llc.redstone.htslreborn.ui.browser

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.nio.file.ClosedWatchServiceException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

object FileExplorerHandler {

    val fileEditCooldown = mutableMapOf<String, Long>()
    val fileEventQueue = ConcurrentLinkedQueue<Path>()
    val watcher: WatchService = FileSystems.getDefault().newWatchService()
    private var watchedDir: Path = FileHandler.currentDir
    private var watchedKey: WatchKey = registerWatchedDir(watchedDir)

    fun init() {
        if (!watchedDir.exists()) watchedDir.createDirectories()

        Thread {
            try {
                while (true) {
                    val key = watcher.take()
                    val watchable = key.watchable() as Path
                    for (event in key.pollEvents()) {
                        val contextPath = event.context() as Path
                        fileEventQueue.offer(watchable.resolve(contextPath))
                    }
                    if (!key.reset()) break
                }
            } catch (e: ClosedWatchServiceException) {
                // Watcher closed on shutdown
            } catch (e: InterruptedException) {
                // Watcher interrupted on shutdown
            }
        }.apply {
            name = "HTSL Reborn File Watcher"
            isDaemon = true // Non-daemon threads block the client from exiting
        }.start()

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            while (true) {
                val path = fileEventQueue.poll() ?: break
                val name = path.fileName.toString()
                val now = System.currentTimeMillis()
                val lastEdit = fileEditCooldown[name]
                if (lastEdit != null && now - lastEdit < 100) continue
                fileEditCooldown[name] = now

                FileHandler.refreshFiles()

            }
        })
    }

    fun registerWatchedDir(path: Path): WatchKey {
        if (!watchedDir.exists()) watchedDir.createDirectories()
        return try {
            path.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to register watcher for $path: ${e.message}")
        }
    }

    fun setWatchedDir(path: Path) {
        if (!path.exists()) path.createDirectories()
        require(path.isDirectory()) { "Path must be a directory" }

        watchedKey.cancel()
        watchedKey = registerWatchedDir(path)
        watchedDir = path
    }
}