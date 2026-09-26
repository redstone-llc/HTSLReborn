package llc.redstone.htslreborn.config

import com.google.gson.GsonBuilder
import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.ui.browser.FileExplorerHandler
import llc.redstone.htslreborn.ui.browser.FileHandler
import llc.redstone.htslreborn.utils.ToastUtils
import net.minecraft.network.chat.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.name

object HTSLConfig {
    val data = HTSLConfigData()
    var pendingDelete: Path? = null

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file: Path
        get() = HTSLReborn.MC.gameDirectory.toPath().resolve("config/htslreborn.json")

    fun load() {
        if (!Files.isRegularFile(file)) return
        runCatching {
            val loaded = gson.fromJson(Files.readString(file), HTSLConfigData::class.java) ?: return
            data.fileDeletionConfirmation = loaded.fileDeletionConfirmation
            data.importsDirectory = loaded.importsDirectory?.ifBlank { "htsl" } ?: "htsl"
            data.guiTimeout = loaded.guiTimeout.coerceIn(250, 60_000)
            data.inputTimeout = loaded.inputTimeout.coerceIn(250, 60_000)
            data.itemTimeout = loaded.itemTimeout.coerceIn(250, 60_000)
            data.commandTimeout = loaded.commandTimeout.coerceIn(250, 60_000)
            data.playCompleteSound = loaded.playCompleteSound
            data.silenceImportMessages = loaded.silenceImportMessages
            data.silenceImportSounds = loaded.silenceImportSounds
        }.onFailure {
            HTSLReborn.LOGGER.warn("Failed to read {}", file, it)
        }
    }

    fun save() {
        data.guiTimeout = data.guiTimeout.coerceIn(250, 60_000)
        data.inputTimeout = data.inputTimeout.coerceIn(250, 60_000)
        data.itemTimeout = data.itemTimeout.coerceIn(250, 60_000)
        data.commandTimeout = data.commandTimeout.coerceIn(250, 60_000)
        if (data.importsDirectory.isBlank()) data.importsDirectory = "htsl"
        runCatching {
            Files.createDirectories(file.parent)
            Files.writeString(file, gson.toJson(data))
        }.onFailure {
            HTSLReborn.LOGGER.warn("Failed to write {}", file, it)
        }
        applyImportsDirectory()
    }

    fun importsPath(): Path {
        val raw = Path(data.importsDirectory.ifBlank { "htsl" })
        return if (raw.isAbsolute) raw.normalize() else HTSLReborn.MC.gameDirectory.toPath().resolve(raw).normalize()
    }

    fun applyImportsDirectory() {
        val next = runCatching { importsPath() }.getOrElse { return }
        if (runCatching { Files.createDirectories(next) }.isFailure) return
        val previous = FileHandler.baseDir
        FileHandler.baseDir = next
        if (previous != next || !FileHandler.currentDir.startsWith(next)) {
            FileHandler.currentDir = next
            FileExplorerHandler.setWatchedDir(next)
        }
        FileHandler.refreshFiles()
    }

    fun requestDelete(path: Path) {
        if (!data.fileDeletionConfirmation || pendingDelete == path) {
            pendingDelete = null
            performDelete(path)
            return
        }
        pendingDelete = path
        ToastUtils.send(
            Component.translatable("htslreborn.config.delete.confirm.title"),
            Component.translatable("htslreborn.config.delete.confirm.description", path.name)
        )
    }

    fun performDelete(path: Path): Boolean {
        if (path == FileHandler.baseDir) return false
        val deleted = runCatching {
            if (path.isDirectory()) path.toFile().deleteRecursively() else path.deleteIfExists()
        }.getOrDefault(false)
        if (!deleted) return false
        if (FileHandler.currentDir.startsWith(path)) {
            FileHandler.currentDir = FileHandler.baseDir
            FileExplorerHandler.setWatchedDir(FileHandler.baseDir)
        }
        FileHandler.refreshFiles()
        return true
    }
}

class HTSLConfigData {
    var fileDeletionConfirmation: Boolean = true
    var importsDirectory: String = "htsl"
    var guiTimeout: Int = 5_000
    var inputTimeout: Int = 5_000
    var itemTimeout: Int = 5_000
    var commandTimeout: Int = 1_000
    var playCompleteSound: Boolean = true
    var silenceImportMessages: Boolean = true
    var silenceImportSounds: Boolean = false
}
