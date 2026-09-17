package llc.redstone.htslreborn.ui

import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class WorkingTopbarWidget(x: Int, y: Int) : IconWidget(
    x, y, 176, 17, Component.literal("topbar")
) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/topbar/topbar_disabled.png")
    }

    override val BACKGROUND: Identifier
        get() = WorkingTopbarWidget.BACKGROUND

    override val icons = listOf(
        Icon(1, 0) {},
        Icon(16, 0) {},
        Icon(31, 0) {},
        Icon(46, 0) {},
        Icon(61, 0) {},
        Icon(145, 0) {

        },
        Icon(160, 0) {

        }
    )
}