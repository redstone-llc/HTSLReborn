package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.mixin.ui.access.ClickableWidgetAccessor
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.gui.Click

class DropdownComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing
) : FlowLayout(horizontalSizing, verticalSizing, Algorithm.VERTICAL) {
    var isVisible = false
    var actionComponent: ButtonComponent? = null
    var x = 0
    var y = 0

    fun handleDropdownButton(buttonComponent: ButtonComponent?) {
        isVisible = !isVisible
        x = buttonComponent!!.x() - 5
        y = buttonComponent.y() - 5 - this.height()
        positioning(Positioning.absolute(x, y))
    }

    fun actionComponent(buttonComponent: ButtonComponent): DropdownComponent {
        this.actionComponent = buttonComponent
        return this
    }

    override fun draw(context: OwoUIDrawContext?, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        if (!isVisible) return
        super.draw(context, mouseX, mouseY, partialTicks, delta)
    }

    override fun onMouseDown(click: Click, doubled: Boolean): Boolean {
        if (!isVisible) return false
        val component = this.childAt(click.x.toInt() + x, click.y.toInt() + y) as? ButtonComponent
        val accessor = component as? ClickableWidgetAccessor
        if (component != null && accessor != null) {
            // TODO: This isn't what we want; just run the specified action, no switching out the actual button
//            actionComponent?.message = component.message
//            actionComponent?.setTooltip(accessor.`owo$getTooltip`()?.tooltip)
            isVisible = false
            return true
        }
        return super.onMouseDown(click, doubled)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing): DropdownComponent {
            return DropdownComponent(horizontalSizing, verticalSizing)
        }
    }
}