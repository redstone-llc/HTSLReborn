package llc.redstone.htslreborn.config;

import io.wispforest.owo.config.annotation.Config
import io.wispforest.owo.config.annotation.Modmenu


@Modmenu(modId = "htslreborn")
@Config(name = "htslreborn", wrapperName = "HtslConfig")
class HtslConfigModel {
    var defaultImportStrategy: ImportStrategy = ImportStrategy.APPEND
    enum class ImportStrategy {
        APPEND, REPLACE, UPDATE
    }
}