package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.ui.FileBrowserHandler
import net.minecraft.client.gui.Click

class FocusableFlowLayout(
    horizontalSizing: Sizing, verticalSizing: Sizing, private val index: Int
): FlowLayout(horizontalSizing, verticalSizing, Algorithm.HORIZONTAL) {
    var isFocused = false

    override fun draw(context: OwoUIDrawContext?, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        super.draw(context, mouseX, mouseY, partialTicks, delta)
        if (isFocused) {
            this.drawFocusHighlight(context, mouseX, mouseY, partialTicks, delta)
        }
    }

    override fun onMouseDown(click: Click, doubled: Boolean): Boolean {
        if (doubled && FileBrowserHandler.handleDirectoryClick(index)) {
            return true
        }

        isFocused = !isFocused

        parent?.children()?.filterIsInstance<FocusableFlowLayout>()?.forEach {
            if (it != this) {
                it.isFocused = false
            }
        }

        return super.onMouseDown(click, doubled)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int): FocusableFlowLayout {
            return FocusableFlowLayout(horizontalSizing, verticalSizing, index)
        }
    }
}