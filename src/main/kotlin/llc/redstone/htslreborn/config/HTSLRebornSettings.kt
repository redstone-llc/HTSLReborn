@file:Suppress("UnstableApiUsage")

package llc.redstone.htslreborn.config

import com.mojang.serialization.Codec
import dev.isxander.yacl3.config.v3.JsonFileCodecConfig
import dev.isxander.yacl3.config.v3.register
import dev.isxander.yacl3.config.v3.value
import llc.redstone.htslreborn.HTSLReborn.MOD_ID
import net.fabricmc.loader.api.FabricLoader

object HTSLRebornSettings: JsonFileCodecConfig<HTSLRebornSettings>(
    FabricLoader.getInstance().configDir.resolve("${MOD_ID}.json")
) {
    val showFileExplorer by register<Boolean>(default = true, BOOL)
    val saveFileExplorerState by register<Boolean>(default = true, BOOL)

    val guiTimeout by register<Int>(default = 60, INT)
    val clickDelay by register<Int>(default = 0, INT)
    val cancelSoundsDuringImport by register<Boolean>(default = true, BOOL)
    val playCompleteSound by register<Boolean>(default = true, BOOL)

    private val _firstLaunch by register<Boolean>(default = true, BOOL)
    var firstLaunch = false
        private set

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