package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.utils.ItemConvertUtils
import net.minecraft.item.ItemStack
import java.io.File
import kotlin.text.lowercase

object FileHandler {
    internal var files = mutableListOf<String>()
    internal var filteredFiles = mutableListOf<String>()
    internal var page = 0
    internal val cachedItems = mutableMapOf<String, ItemStack?>()
    internal var selectedIndex = -1
    internal var subDir = ""
    internal var search = ""

    private val importDir = File("HTSL/imports")
    val itemExtensions = listOf(".json", ".nbt")
    val htslExtensions = listOf(".htsl")

    fun currentDir(): File {
        return if (subDir.isEmpty()) {
            importDir
        } else {
            File(importDir, subDir)
        }
    }

    fun refreshFiles(live: Boolean = false) {
        var live = live
        if (!importDir.exists()) {
            importDir.mkdirs()
        }
        var baseDir = currentDir()
        if (!baseDir.exists()) {
            subDir = ""
            baseDir = importDir
            live = true
        }

        files = baseDir.listFiles().filter {
            if (it.isDirectory) {
                true
            } else {
                val name = it.name.lowercase()
                itemExtensions.any { ext -> name.endsWith(ext) } ||
                        htslExtensions.any { ext -> name.endsWith(ext)  }
            }
        }.map { it.name }.toMutableList()
        files.sortWith(compareBy({ !File(baseDir, it).isDirectory }, { it.lowercase() }))
        filteredFiles = files.toMutableList()
        page = 0
        cachedItems.clear()

        if (search.isNotEmpty()) {
            val query = search.lowercase()
            filteredFiles = files.filter { it.lowercase().contains(query) }.toMutableList()
        } else {
            filteredFiles = files.toMutableList()
        }

        if (!live) {
            FileExplorer.INSTANCE.focus = null
            FileExplorer.INSTANCE.updateButtons()
        }
    }

    fun getFile(fileName: String): File {
        val baseDir = File("HTSL/imports", subDir)
        return File(baseDir, fileName)
    }

    fun getItemForFile(file: File): ItemStack? {
        return cachedItems.getOrPut(file.name) {
            try {
                return@getOrPut ItemConvertUtils.fileToItemStack(file)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}