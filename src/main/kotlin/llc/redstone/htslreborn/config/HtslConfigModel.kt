package llc.redstone.htslreborn.config

import io.wispforest.owo.config.annotation.Config
import io.wispforest.owo.config.annotation.Hook
import io.wispforest.owo.config.annotation.Modmenu
import io.wispforest.owo.config.annotation.SectionHeader
import llc.redstone.htslreborn.HTSLReborn.MOD_ID


@Modmenu(modId = MOD_ID)
@Config(name = MOD_ID, wrapperName = "HtslConfig")
class HtslConfigModel {

    @SectionHeader("explorer")
    @JvmField
    var showFileExplorer: Boolean = true

    @JvmField
    @Hook
    var importsDirectory: String = "htsl/imports"

    @SectionHeader("import")
    @JvmField
    var defaultImportStrategy: ImportStrategy = ImportStrategy.APPEND
    enum class ImportStrategy {
        APPEND, REPLACE, /*UPDATE*/
    }

    @JvmField
    var playCompleteSound: Boolean = true

    @JvmField
    var silenceImportMessages: Boolean = true

    @JvmField
    var silenceImportSounds: Boolean = true

    @SectionHeader("advanced")
    @JvmField
    var disablesJSSandboxing: Boolean = false
}