package llc.redstone.htslreborn.ui.working

import llc.redstone.htslreborn.ui.Icon
import llc.redstone.htslreborn.ui.IconWidget
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class WorkingTopbarWidget(x: Int, y: Int) : IconWidget(
    x, y, 223, 17, Component.literal("topbar")
) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/topbar/topbar_disabled.png")
    }

    override val BACKGROUND: Identifier
        get() = WorkingTopbarWidget.BACKGROUND

    override val icons = listOf(
        Icon(1, 0, disabled = true) {},
        Icon(16, 0, disabled = true) {},
        Icon(31, 0, disabled = true) {},
        Icon(46, 0, disabled = true) {},
        Icon(61, 0, disabled = true) {},
        Icon(191, 0) {

        },
        Icon(206, 0) {

        }
    )
}