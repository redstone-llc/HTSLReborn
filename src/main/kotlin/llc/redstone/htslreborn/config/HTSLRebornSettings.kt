package llc.redstone.htslreborn.config

import com.mojang.serialization.Codec
import dev.isxander.yacl3.config.v3.JsonFileCodecConfig
import dev.isxander.yacl3.config.v3.register
import dev.isxander.yacl3.config.v3.value
import net.fabricmc.loader.api.FabricLoader

open class HTSLRebornSettings(): JsonFileCodecConfig<HTSLRebornSettings>(
    FabricLoader.getInstance().configDir.resolve("htslreborn.json")
) {
    val showFileExplorer by register<Boolean>(default = true, Codec.BOOL)
    val saveFileExplorerState by register<Boolean>(default = true, Codec.BOOL)

    val guiTimeout by register<Int>(default = 60, Codec.INT)
    val clickDelay by register<Int>(default = 0, Codec.INT)
    val cancelSoundsDuringImport by register<Boolean>(default = true, Codec.BOOL)
    val playCompleteSound by register<Boolean>(default = true, Codec.BOOL)

    var firstLaunch = false
    val _firstLaunch by register<Boolean>(default = true, Codec.BOOL)

    final val allSettings = arrayOf(
        showFileExplorer,
        saveFileExplorerState,
        guiTimeout,
        clickDelay,
        cancelSoundsDuringImport,
        playCompleteSound,
        _firstLaunch
    )

    constructor(settings: HTSLRebornSettings) : this() {
        this.showFileExplorer.value = settings.showFileExplorer.value
        this.saveFileExplorerState.value = settings.saveFileExplorerState.value
        this.guiTimeout.value = settings.guiTimeout.value
        this.clickDelay.value = settings.clickDelay.value
        this.playCompleteSound.value = settings.playCompleteSound.value
        this.cancelSoundsDuringImport.value = settings.cancelSoundsDuringImport.value
        this._firstLaunch.value = settings._firstLaunch.value
    }

    companion object : HTSLRebornSettings() {
        init {
            if (!loadFromFile()) {
                saveToFile()
            }

            if (_firstLaunch.value) {
                firstLaunch = true
                _firstLaunch.value = false
                saveToFile()
            }
        }
    }
}