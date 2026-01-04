package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.ui.FileHandler.search

object FileBrowserHandler {
    fun onSearchChanged(newSearch: String) {
        if (newSearch != search) {
            search = newSearch
            FileHandler.refreshFiles()
            FileBrowser.INSTANCE.refreshExplorer()

        }
    }

    fun onBreadcrumbClicked(name: String, index: Int) {
        if (name == "imports") {
            FileHandler.subDir = ""
        } else {
            val dir = ("imports/" + FileHandler.subDir)
                .split("/")
                .subList(0, index + 1)
                .joinToString("/")
            FileHandler.subDir = dir.removePrefix("imports/")
        }
        //TODO: there was a concurrent modification exception here :D
        FileHandler.refreshFiles()
        FileBrowser.INSTANCE.refreshExplorer()
        FileBrowser.INSTANCE.refreshBreadcrumbs()
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