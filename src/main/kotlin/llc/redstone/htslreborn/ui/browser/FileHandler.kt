package llc.redstone.htslreborn.ui.browser

import llc.redstone.htslreborn.utils.ItemUtils
import net.minecraft.world.item.ItemStack
import java.nio.file.Path
import kotlin.io.path.*

object FileHandler {
    internal var files = mutableListOf<Path>()
    internal var filteredFiles = mutableListOf<Path>()
    internal var page = 0
    internal val cachedItems = mutableMapOf<String, ItemStack?>()
    internal var search = ""

    internal var baseDir = Path("htsl")
    internal var currentDir = baseDir

    val itemExtensions = listOf("nbt")
    val htslExtensions = listOf("htsl")

    fun refreshFiles() {
        if (!currentDir.exists()) {
            currentDir.createDirectories()
        }

        files = currentDir.listDirectoryEntries().asSequence().filter {
            if (it.isDirectory()) {
                true
            } else {
                val name = it.name.lowercase()
                itemExtensions.any { ext -> name.endsWith(".$ext") } ||
                htslExtensions.any { ext -> name.endsWith(".$ext") }
            }
        }.sortedWith(compareBy(
            { !it.isDirectory() },
            { it.name.lowercase() }
        )).toMutableList()

        page = 0
        cachedItems.clear()
        filteredFiles = files.filter { matchesSearch(it.name) }.toMutableList()
    }

    fun matchesSearch(name: String): Boolean {
        if (search.isEmpty()) return true
        return name.contains(search, ignoreCase = true)
    }

    fun getItemForFile(path: Path): ItemStack? {
        return cachedItems.getOrPut(path.name) {
            try {
                return@getOrPut ItemUtils.fileToItemStack(path)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}