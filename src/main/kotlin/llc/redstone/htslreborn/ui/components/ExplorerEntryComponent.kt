package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.CursorStyle
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileExplorerHandler
import net.minecraft.client.gui.Click
import net.minecraft.util.Identifier

abstract class ExplorerEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, open val index: Int
): FlowLayout(horizontalSizing, verticalSizing, Algorithm.HORIZONTAL) {

    init {
        mouseEnter().subscribe {
            cursorStyle(CursorStyle.HAND)
        }
    }

    abstract val icon: Identifier
    abstract fun buildContextButtons(): List<Component>

    override fun child(child: Component?): FlowLayout? {
        mouseEnter().subscribe {
            cursorStyle(CursorStyle.HAND)
        }
        return super.child(child)
    }

    override fun children(children: Collection<Component?>?): FlowLayout? {
        children?.forEach {
            it?.mouseEnter()?.subscribe {
                it.cursorStyle(CursorStyle.HAND)
            }
        }
        return super.children(children)
    }

    override fun draw(context: OwoUIDrawContext?, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        super.draw(context, mouseX, mouseY, partialTicks, delta)
        if (FileExplorer.INSTANCE.focus == this) this.drawFocusHighlight(context, mouseX, mouseY, partialTicks, delta)
    }

    override fun onMouseDown(click: Click, doubled: Boolean): Boolean {
        if (doubled && FileExplorerHandler.handleDirectoryClick(index)) return true
        if (doubled && FileExplorerHandler.handleScriptClick(index)) return true

        if (FileExplorer.INSTANCE.focus == this) {
            FileExplorer.INSTANCE.focus = null
        } else {
            FileExplorer.INSTANCE.focus = this
        }

        FileExplorer.INSTANCE.updateButtons()

        return super.onMouseDown(click, doubled)
    }
}