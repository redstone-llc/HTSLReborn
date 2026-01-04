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
    fun refreshFiles() {
        val baseDir = File("HTSL/imports", subDir)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        files = baseDir.listFiles().filter {
            if (it.isDirectory) {
                true
            } else {
                it.name.endsWith(".htsl", true) || it.name.endsWith(".nbt", true)
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
    }

    fun getFile(fileName: String): File {
        val baseDir = File("HTSL/imports", subDir)
        return File(baseDir, fileName)
    }

    fun getItemForFile(fileDir: String, file: File): ItemStack? {
        return cachedItems.getOrPut(fileDir) {
            try {
                return@getOrPut ItemConvertUtils.fileToItemStack(file)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}