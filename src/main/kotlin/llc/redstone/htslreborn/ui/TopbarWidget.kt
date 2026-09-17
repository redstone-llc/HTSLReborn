package llc.redstone.htslreborn.ui

import kotlinx.coroutines.runBlocking
import llc.redstone.htslreborn.utils.CommandUtils
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class TopbarWidget(x: Int, y: Int) : IconWidget(
    x, y, 176, 17, Component.literal("topbar")
) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/topbar/topbar.png")
    }

    override val BACKGROUND: Identifier
        get() = TopbarWidget.BACKGROUND

    override val icons = listOf(
        Icon(1, 0) {
            runBlocking { CommandUtils.runCommand("menu") }
        },
        Icon(16, 0) {
            runBlocking { CommandUtils.runCommand("functions") }
        },
        Icon(31, 0) {
            runBlocking { CommandUtils.runCommand("eventactions") }
        },
        Icon(46, 0) {
            runBlocking { CommandUtils.runCommand("regions") }
        },
        Icon(61, 0) {
            runBlocking { CommandUtils.runCommand("commands") }
        },
        Icon(145, 0) {

        },
        Icon(160, 0) {

        }
    )
}