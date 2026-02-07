package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn.CONFIG
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.config.HtslConfigModel
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileHandler.htslExtensions
import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.htslreborn.utils.RenderUtils.isInitialized
import llc.redstone.systemsapi.importer.ActionContainer
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
        val offset = FileHandler.baseDir.nameCount

        try {
            val current = FileHandler.currentDir
            FileHandler.currentDir = if (index <= -1) {
                FileHandler.baseDir
            } else {
                current.subpath(0, index + offset)
            }

            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
            FileExplorer.INSTANCE.refreshBreadcrumbs()
            setWatchedDir(FileHandler.currentDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Index of the
    fun handleDirectoryClick(index: Int): Boolean {
        val filteredFiles = FileHandler.filteredFiles
        val file = filteredFiles[index]
        if (file.isDirectory()) {
            FileHandler.currentDir = file
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
            FileExplorer.INSTANCE.refreshBreadcrumbs()
            setWatchedDir(FileHandler.currentDir)
            return true
        }
        return false
    }

    fun handleScriptClick(index: Int): Boolean {
        val filteredFiles = FileHandler.filteredFiles
        val file = filteredFiles[index]
        if (file.isRegularFile() && htslExtensions.contains(file.extension)) {
            val method = when (CONFIG.defaultImportStrategy) {
                HtslConfigModel.ImportStrategy.APPEND -> ActionContainer::addActions
                HtslConfigModel.ImportStrategy.REPLACE -> ActionContainer::setActions
                HtslConfigModel.ImportStrategy.UPDATE -> ActionContainer::updateActions
            }

            importingFile = file
            FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.IMPORT, file.name)
            HTSLImporter.importFile(file, method) {
                FileExplorer.INSTANCE.hideWorkingScreen()
            }
            return true
        }
        return false
    }
}