package llc.redstone.htslreborn.config

import dev.isxander.yacl3.api.ConfigCategory
import dev.isxander.yacl3.api.Option
import dev.isxander.yacl3.api.OptionDescription
import dev.isxander.yacl3.api.OptionGroup
import dev.isxander.yacl3.api.YetAnotherConfigLib
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder
import dev.isxander.yacl3.api.controller.StringControllerBuilder
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder
import llc.redstone.htslreborn.HTSLReborn
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

object HTSLConfigScreen {
    fun open(parent: Screen?) {
        val screen = create(parent)
        //? if >=26.1.2 {
        /*HTSLReborn.MC.gui.setScreen(screen)
        *///?} else {
        HTSLReborn.MC.setScreen(screen)
        //?}
    }

    fun create(parent: Screen?): Screen {
        val data = HTSLConfig.data
        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("htslreborn.config.title"))
            .category(filesCategory(data))
            .category(importerCategory(data))
            .save(HTSLConfig::save)
            .build()
            .generateScreen(parent)
    }

    private fun filesCategory(data: HTSLConfigData): ConfigCategory {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("htslreborn.config.category.files"))
            .option(booleanOption(
                "htslreborn.config.file_deletion_confirmation",
                true,
                { data.fileDeletionConfirmation },
                { data.fileDeletionConfirmation = it }
            ))
            .option(
                Option.createBuilder<String>()
                    .name(Component.translatable("htslreborn.config.imports_directory"))
                    .description(description("htslreborn.config.imports_directory"))
                    .binding("htsl", { data.importsDirectory }, { data.importsDirectory = it })
                    .controller(StringControllerBuilder::create)
                    .build()
            )
            .build()
    }

    private fun importerCategory(data: HTSLConfigData): ConfigCategory {
        return ConfigCategory.createBuilder()
            .name(Component.translatable("htslreborn.config.category.importer"))
            .group(
                OptionGroup.createBuilder()
                    .name(Component.translatable("htslreborn.config.group.timeouts"))
                    .option(timeoutOption("htslreborn.config.timeout.gui", 5_000, { data.guiTimeout }, { data.guiTimeout = it }))
                    .option(timeoutOption("htslreborn.config.timeout.input", 5_000, { data.inputTimeout }, { data.inputTimeout = it }))
                    .option(timeoutOption("htslreborn.config.timeout.item", 5_000, { data.itemTimeout }, { data.itemTimeout = it }))
                    .option(timeoutOption("htslreborn.config.timeout.command", 1_000, { data.commandTimeout }, { data.commandTimeout = it }))
                    .build()
            )
            .option(booleanOption(
                "htslreborn.config.play_complete_sound",
                true,
                { data.playCompleteSound },
                { data.playCompleteSound = it }
            ))
            .option(booleanOption(
                "htslreborn.config.silence_import_messages",
                true,
                { data.silenceImportMessages },
                { data.silenceImportMessages = it }
            ))
            .option(booleanOption(
                "htslreborn.config.silence_import_sounds",
                false,
                { data.silenceImportSounds },
                { data.silenceImportSounds = it }
            ))
            .build()
    }

    private fun booleanOption(
        key: String,
        default: Boolean,
        getter: () -> Boolean,
        setter: (Boolean) -> Unit,
    ): Option<Boolean> {
        return Option.createBuilder<Boolean>()
            .name(Component.translatable(key))
            .description(description(key))
            .binding(default, getter, setter)
            .controller(TickBoxControllerBuilder::create)
            .build()
    }

    private fun timeoutOption(key: String, default: Int, getter: () -> Int, setter: (Int) -> Unit): Option<Int> {
        return Option.createBuilder<Int>()
            .name(Component.translatable(key))
            .description(description(key))
            .binding(default, getter, setter)
            .controller { option ->
                IntegerSliderControllerBuilder.create(option).range(250, 30_000).step(250)
            }
            .build()
    }

    private fun description(key: String): OptionDescription {
        return OptionDescription.of(Component.translatable("$key.description"))
    }
}
