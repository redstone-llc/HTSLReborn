package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.htslreborn.utils.RenderUtils.isInitialized
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.io.path.*

object FileExplorerHandler {

    val fileEditCooldown = mutableMapOf<String, Long>()
    val fileEventQueue = ConcurrentLinkedQueue<Path>()
    val watcher: WatchService = FileSystems.getDefault().newWatchService()
    private var watchedDir: Path = FileHandler.currentDir
    private var watchedKey: WatchKey = registerWatchedDir(watchedDir)

    fun init() {
        if (!watchedDir.exists()) watchedDir.createDirectories()

        Thread {
            while (true) {
                val key = watcher.take()
                val watchable = key.watchable() as Path
                for (event in key.pollEvents()) {
                    val contextPath = event.context() as Path
                    fileEventQueue.offer(watchable.resolve(contextPath))
                }
                if (!key.reset()) break
            }
        }.start()

        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick {
            while (true) {
                val path = fileEventQueue.poll() ?: break
                val name = path.fileName.toString()
                val now = System.currentTimeMillis()
                val lastEdit = fileEditCooldown[name]
                if (lastEdit != null && now - lastEdit < 100) continue
                fileEditCooldown[name] = now

                if (FileExplorer.INSTANCE.isInitialized()) {
                    FileHandler.refreshFiles(true)
                    FileExplorer.INSTANCE.refreshExplorer(true)
                }
            }
        })
    }

    fun registerWatchedDir(path: Path): WatchKey {
        if (!path.exists()) path.createDirectories()
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


    fun onSearchChanged(newSearch: String) {
        if (newSearch != search) {
            search = newSearch
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        }
    }

    fun onBreadcrumbClicked(index: Int) {
        try {
            val current = FileHandler.currentDir
            val relative = FileHandler.baseDir.relativize(current)
            FileHandler.currentDir = if (index <= -1) {
                FileHandler.baseDir
            } else if (index <= 0 || relative.nameCount == 0) {
                FileHandler.baseDir
            } else {
                FileHandler.baseDir.resolve(relative.subpath(0, index.coerceAtMost(relative.nameCount))).normalize()
            }

            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
            FileExplorer.INSTANCE.refreshBreadcrumbs()
            setWatchedDir(FileHandler.currentDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}
