package llc.redstone.htslreborn.ui

import kotlinx.coroutines.runBlocking
import llc.redstone.htslreborn.ui.HTSLScreen.Companion.notBrowsing
import llc.redstone.htslreborn.utils.CommandUtils
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class TopbarWidget(x: Int, y: Int, val wide: Boolean = false, val containersActive: Boolean = true) : IconWidget(
    x, y, if (wide) 223 else 176, 17, Component.translatable("htslreborn.topbar")
) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/topbar/topbar.png")
        val BACKGROUND_WIDE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/topbar/topbar_wide.png")
        val BACKGROUND_DISABLED =
            Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/topbar/topbar_disabled.png")
    }

    override val BACKGROUND: Identifier
        get() = when {
            !containersActive -> BACKGROUND_DISABLED
            wide -> BACKGROUND_WIDE
            else -> TopbarWidget.BACKGROUND
        }

    override val icons = listOf(
        Icon(1, 0, disabled = !containersActive, tooltip = Component.translatable("htslreborn.topbar.housing_menu")) {
            if (containersActive) runBlocking {
                notBrowsing()
                CommandUtils.runCommand("menu")
            }
        },
        Icon(16, 0, disabled = !containersActive, tooltip = Component.translatable("htslreborn.topbar.functions")) {
            if (containersActive) runBlocking {
                notBrowsing()
                CommandUtils.runCommand("functions")
            }
        },
        Icon(31, 0, disabled = !containersActive, tooltip = Component.translatable("htslreborn.topbar.event_actions")) {
            if (containersActive) runBlocking {
                notBrowsing()
                CommandUtils.runCommand("eventactions")
            }
        },
        Icon(46, 0, disabled = !containersActive, tooltip = Component.translatable("htslreborn.topbar.regions")) {
            if (containersActive) runBlocking {
                notBrowsing()
                CommandUtils.runCommand("regions")
            }
        },
        Icon(61, 0, disabled = !containersActive, tooltip = Component.translatable("htslreborn.topbar.commands")) {
            if (containersActive) runBlocking {
                notBrowsing()
                CommandUtils.runCommand("commands")
            }
        },
        Icon(if (wide) 192 else 145, 0, tooltip = Component.translatable("htslreborn.topbar.export")) {
            HTSLScreen.export()
        },
        Icon(if (wide) 207 else 160, 0, tooltip = Component.translatable("htslreborn.topbar.import")) {
            HTSLScreen.import()
        }
    )
}