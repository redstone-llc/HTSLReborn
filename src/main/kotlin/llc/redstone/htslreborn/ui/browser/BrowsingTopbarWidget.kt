package llc.redstone.htslreborn.ui.browser

import kotlinx.coroutines.runBlocking
import llc.redstone.htslreborn.ui.HTSLScreen
import llc.redstone.htslreborn.ui.Icon
import llc.redstone.htslreborn.ui.IconWidget
import llc.redstone.htslreborn.utils.CommandUtils
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class BrowsingTopbarWidget(x: Int, y: Int) : IconWidget(
    x, y, 223, 17, Component.literal("topbar")
) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/topbar/topbar_wide.png")
    }

    override val BACKGROUND: Identifier
        get() = BrowsingTopbarWidget.BACKGROUND

    override val icons = listOf(
        Icon(1, 0) {
            runBlocking { CommandUtils.runCommand("menu") }
            HTSLScreen.isBrowsing = false
        },
        Icon(16, 0) {
            runBlocking { CommandUtils.runCommand("functions") }
            HTSLScreen.isBrowsing = false
        },
        Icon(31, 0) {
            runBlocking { CommandUtils.runCommand("eventactions") }
            HTSLScreen.isBrowsing = false
        },
        Icon(46, 0) {
            runBlocking { CommandUtils.runCommand("regions") }
            HTSLScreen.isBrowsing = false
        },
        Icon(61, 0) {
            runBlocking { CommandUtils.runCommand("commands") }
            HTSLScreen.isBrowsing = false
        },
        Icon(192, 0) { //idk why these numbers are different
            HTSLScreen.export()
        },
        Icon(207, 0) { //idk why these numbers are different
            HTSLScreen.import()
        }
    )
}