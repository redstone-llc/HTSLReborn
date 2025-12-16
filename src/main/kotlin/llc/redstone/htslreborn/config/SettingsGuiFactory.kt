package dev.wekend.housingtoolbox.config

import dev.isxander.yacl3.config.v3.register
import dev.isxander.yacl3.dsl.*
import llc.redstone.htslreborn.HTSLReborn
import net.minecraft.client.gui.screen.Screen

fun createSettingsGui(parent: Screen?) = SettingsGuiFactory().createSettingsGui(parent)

private class SettingsGuiFactory {
    val settings = HTSLRebornSettings(HTSLRebornSettings)

    fun createSettingsGui(parent: Screen?) = YetAnotherConfigLib(HTSLReborn.MOD_ID) {
        save(HTSLRebornSettings::saveToFile)

        val input by categories.registering {
            val fileExplorer by groups.registering {
                options.register(HTSLRebornSettings.showFileExplorer) {
                    defaultDescription()
                    controller = tickBox()
                }
                options.register(HTSLRebornSettings.saveFileExplorerState) {
                    defaultDescription()
                    controller = tickBox()
                }
            }

            val importingExporting by groups.registering {
                options.register(HTSLRebornSettings.guiTimeout) {
                    defaultDescription()
                    controller = integerSlider() // this is probably wrong
                }
                options.register(HTSLRebornSettings.clickDelay) {
                    defaultDescription()
                    controller = integerSlider() // this is probably wrong
                }
                options.register(HTSLRebornSettings.cancelSoundsDuringImport) {
                    defaultDescription()
                    controller = tickBox()
                }
                options.register(HTSLRebornSettings.playCompleteSound) {
                    defaultDescription()
                    controller = tickBox()
                }
            }
        }
    }.generateScreen(parent)

}

private fun OptionDsl<*>.defaultDescription() {
    descriptionBuilder {
        addDefaultText()
    }
}

private fun ButtonOptionDsl.defaultDescription() {
    descriptionBuilder {
        addDefaultText()
    }
}