@file:Suppress("UnstableApiUsage")

package llc.redstone.htslreborn.config

import com.mojang.serialization.Codec
import com.mojang.serialization.DataResult
import dev.isxander.yacl3.config.v3.JsonFileCodecConfig
import dev.isxander.yacl3.config.v3.register
import dev.isxander.yacl3.config.v3.value
import llc.redstone.htslreborn.HTSLReborn.MOD_ID
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.text.Text

object HTSLRebornSettings: JsonFileCodecConfig<HTSLRebornSettings>(
    FabricLoader.getInstance().configDir.resolve("${MOD_ID}.json")
) {
    enum class ImportMethod(val displayName: Text) {
        ADD(Text.translatable("htslreborn.menu.button.import.add")),
        REPLACE(Text.translatable("htslreborn.menu.button.import.replace")),
        UPDATE(Text.translatable("htslreborn.menu.button.import.update"))
    }

    private val IMPORT_METHOD_CODEC: Codec<ImportMethod> =
        Codec.STRING.comapFlatMap(
            { raw ->
                val normalized = raw.trim().uppercase()
                runCatching { ImportMethod.valueOf(normalized) }
                    .fold(
                        onSuccess = { DataResult.success(it) },
                        onFailure = {
                            DataResult.error { "Unknown ImportMethod '$raw'. Valid: ${ImportMethod.entries}" }
                        }
                    )
            },
            { it.name } // what gets written to json
        )

    val showFileExplorer by register<Boolean>(default = true, BOOL)
    val defaultImportMethod by register<ImportMethod>(default = ImportMethod.ADD, IMPORT_METHOD_CODEC)

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