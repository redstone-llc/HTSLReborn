package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.htslreborn.ui.components.FileExplorerEntryComponent

object FileBrowserHandler {
    fun getSelectedEntry(): FileExplorerEntryComponent {
        val explorerContent = FileBrowser.INSTANCE.content
        return explorerContent.children()
            .filterIsInstance<FileExplorerEntryComponent>()
            .first { it.isFocused }
    }

    fun onSearchChanged(newSearch: String) {
        if (newSearch != search) {
            search = newSearch
            FileHandler.refreshFiles()
            FileBrowser.INSTANCE.refreshExplorer()

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
                println("Changed directory to: ${FileHandler.subDir} ")
            }
            FileHandler.refreshFiles()
            FileBrowser.INSTANCE.refreshExplorer()
            FileBrowser.INSTANCE.refreshBreadcrumbs()
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
            FileBrowser.INSTANCE.refreshExplorer()
            FileBrowser.INSTANCE.refreshBreadcrumbs()
            return true
        }
        return false
    }
}