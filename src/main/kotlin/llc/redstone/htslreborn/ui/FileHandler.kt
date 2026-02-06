package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.utils.ItemConvertUtils
import net.minecraft.item.ItemStack
import java.nio.file.Path
import kotlin.io.path.*

object FileHandler {
    internal var files = mutableListOf<String>()
    internal var filteredFiles = mutableListOf<String>()
    internal var page = 0
    internal val cachedItems = mutableMapOf<String, ItemStack?>()
    internal var search = ""

    internal var baseDir = Path(HTSLReborn.CONFIG.importsDirectory)
    internal var currentDir = baseDir

    val itemExtensions = listOf("nbt")
    val htslExtensions = listOf("htsl")

    fun refreshFiles(live: Boolean = false) {
        var live = live
        if (!currentDir.exists()) {
            currentDir.createDirectory()
            live = true
        }

        files = currentDir.listDirectoryEntries().asSequence().filter {
            if (it.isDirectory()) {
                true
            } else {
                val name = it.name.lowercase()
                itemExtensions.any { ext -> name.endsWith(ext) } ||
                htslExtensions.any { ext -> name.endsWith(ext) }
            }
        }.sortedWith(compareBy(
            { !it.isDirectory() },
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

    fun getFile(fileName: String): Path {
        return currentDir.resolve(fileName)
    }

    fun getItemForFile(path: Path): ItemStack? {
        return cachedItems.getOrPut(path.name) {
            try {
                return@getOrPut ItemConvertUtils.fileToItemStack(path)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}