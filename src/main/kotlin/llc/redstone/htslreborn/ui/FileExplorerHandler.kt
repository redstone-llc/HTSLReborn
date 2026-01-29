package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn.CONFIG
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.config.HtslConfigModel
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.systemsapi.importer.ActionContainer

object FileExplorerHandler {

    fun onSearchChanged(newSearch: String) {
        if (newSearch != search) {
            search = newSearch
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        }
    }

    fun onBreadcrumbClicked(name: String, index: Int) {
        try {
            if (index == -1) {
                FileHandler.subDir = ""
            } else {
                val dir = (FileHandler.subDir)
                    .split("/")
                    .subList(0, index + 1)
                    .joinToString("/")
                FileHandler.subDir = dir
            }
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
            FileExplorer.INSTANCE.refreshBreadcrumbs()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //Index of the
    fun handleDirectoryClick(index: Int): Boolean {
        val filteredFiles = FileHandler.filteredFiles
        val fileName = filteredFiles[index]
        val file = FileHandler.getFile(fileName)
        if (file.isDirectory) {
            FileHandler.subDir = FileHandler.subDir
                .let { if (it.isEmpty()) file.name else "$it/${file.name}" }
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
            FileExplorer.INSTANCE.refreshBreadcrumbs()
            return true
        }
        return false
    }

    fun handleScriptClick(index: Int): Boolean {
        val filteredFiles = FileHandler.filteredFiles
        val fileName = filteredFiles[index]
        val file = FileHandler.getFile(fileName)
        if (file.isFile && file.extension == "htsl") {
            val method = when (CONFIG.defaultImportStrategy) {
                HtslConfigModel.ImportStrategy.APPEND -> ActionContainer::addActions
                HtslConfigModel.ImportStrategy.REPLACE -> ActionContainer::setActions
                HtslConfigModel.ImportStrategy.UPDATE -> ActionContainer::updateActions
            }
            FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.IMPORT, file.name)
            importingFile = file.name
            HTSLImporter.importFile(file, method) {
                FileExplorer.INSTANCE.hideWorkingScreen()
            }
            return true
        }
        return false
    }
}