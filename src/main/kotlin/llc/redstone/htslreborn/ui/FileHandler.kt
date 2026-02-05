package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.utils.ItemConvertUtils
import net.minecraft.item.ItemStack
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.text.lowercase

object FileHandler {
    internal var files = mutableListOf<String>()
    internal var filteredFiles = mutableListOf<String>()
    internal var page = 0
    internal val cachedItems = mutableMapOf<String, ItemStack?>()
    internal var search = ""

    internal val baseDir = Paths.get("./htsl/imports")
    internal var currentDir = baseDir

    val itemExtensions = listOf(".json", ".nbt")
    val htslExtensions = listOf(".htsl")

    fun refreshFiles(live: Boolean = false) {
        var live = live
        if (!currentDir.exists()) {
            Files.createDirectory(currentDir)
            live = true
        }

        files = Files.list(currentDir).filter {
            if (Files.isDirectory(it)) {
                true
            } else {
                val name = it.name.lowercase()
                itemExtensions.any { ext -> name.endsWith(ext) } ||
                htslExtensions.any { ext -> name.endsWith(ext) }
            }
        }.sorted(compareBy(
            { !Files.isDirectory(it) },
            { it.name.lowercase() }
        )).map { it.name }.toList().toMutableList()

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
        return currentDir.resolve(fileName).toFile()
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