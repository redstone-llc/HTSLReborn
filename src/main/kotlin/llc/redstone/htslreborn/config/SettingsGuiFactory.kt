@file:Suppress("UnstableApiUsage")

package llc.redstone.htslreborn.config

import dev.isxander.yacl3.config.v3.register
import dev.isxander.yacl3.dsl.*
import llc.redstone.htslreborn.HTSLReborn
import net.minecraft.client.gui.screen.Screen

fun createSettingsGui(parent: Screen?): Screen = YetAnotherConfigLib(HTSLReborn.MOD_ID) {
    save(HTSLRebornSettings::saveToFile)

    categories.register("input") {
        val fileExplorer by groups.registering {
            options.register(HTSLRebornSettings.showFileExplorer) {
                defaultDescription()
                controller = tickBox()
            }
        }

        groups.register("importing") {
            options.register(HTSLRebornSettings.playCompleteSound) {
                defaultDescription()
                controller = tickBox()
            }
        }
    }
}.generateScreen(parent)

private fun GroupDsl.defaultDescription(lines: Int? = null) = descriptionBuilder {
    addDefaultText(lines)
}

private fun OptionDsl<*>.defaultDescription(lines: Int? = null) = descriptionBuilder {
    addDefaultText(lines)
}