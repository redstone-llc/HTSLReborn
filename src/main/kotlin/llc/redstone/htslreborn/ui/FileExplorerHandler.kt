package llc.redstone.htslreborn.ui

import io.wispforest.owo.ui.component.ButtonComponent
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.htslio.HTSLExporter
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.htslreborn.ui.components.ExplorerEntryComponent
import llc.redstone.htslreborn.ui.components.FolderEntryComponent
import llc.redstone.htslreborn.ui.components.ItemEntryComponent
import llc.redstone.htslreborn.ui.components.ScriptEntryComponent
import llc.redstone.systemsapi.SystemsAPI
import llc.redstone.systemsapi.importer.ActionContainer
import llc.redstone.systemsapi.util.ItemStackUtils.giveItem
import net.minecraft.text.Text
import net.minecraft.util.Util

object FileExplorerHandler {
    fun onActionClicked(buttonComponent: ButtonComponent) {
        if (buttonComponent.id() == "openFolder") {
            val dir = FileHandler.currentDir()
            Util.getOperatingSystem().open(dir)
            return
        }

        if (buttonComponent.id() == "cancel") {
            SystemsAPI.getHousingImporter().cancelImport()
            return
        }

        when (val focused = FileExplorer.INSTANCE.focus) {
            is FolderEntryComponent -> {
                when (buttonComponent.id()) {
                    "open" -> {
                        Util.getOperatingSystem().open(focused.file)
                    }
                    "delete" -> {
                        focused.file.delete()
                        FileHandler.refreshFiles()
                        FileExplorer.INSTANCE.refreshExplorer()
                    }
                }
            }
            is ItemEntryComponent -> {
                when (buttonComponent.id()) {
                    "giveItem" -> {
                        val item = FileHandler.getItemForFile(focused.file) ?: return
                        val slot = convertSlot(MC.player?.inventory?.emptySlot ?: -1)
                        item.giveItem(slot)
                    }
                    "saveItem" -> {
                        TODO()
                    }
                    "open" -> {
                        Util.getOperatingSystem().open(focused.file)
                    }
                    "delete" -> {
                        focused.file.delete()
                        FileHandler.refreshFiles()
                        FileExplorer.INSTANCE.refreshExplorer()
                    }
                }
            }
            is ScriptEntryComponent -> {
                when (buttonComponent.id()) {
                    "import", "replace" -> {
                        val method = when (buttonComponent.id()) {
                            "import" -> ActionContainer::addActions
                            "replace" -> ActionContainer::setActions
                            "update" -> ActionContainer::updateActions
                            else -> return
                        }
                        FileExplorer.INSTANCE.showImportScreen(focused.file.name)
                        importingFile = focused.file.name
                        HTSLImporter.importFile(focused.file, method) {
                            FileExplorer.INSTANCE.hideImportScreen()
                        }
                    }
                    "update" -> {
                        MC.player?.sendMessage(
                            Text.literal("[HTSL Reborn] Updating actions currently isnt supported.")
                                .withColor(0xFF0000), false
                        )
                    }
                    "export" -> {
                        HTSLExporter.exportFile(focused.file)
                    }
                }
            }
        }
    }

    private fun convertSlot(slot: Int) = when (slot) {
        in 0..8 -> slot + 36
        in 9..35 -> slot
        else -> -1
    }

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
}