package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.CursorStyle
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.ui.FileBrowser
import llc.redstone.htslreborn.ui.FileBrowserHandler
import net.minecraft.client.gui.Click
import net.minecraft.util.Identifier

abstract class FileExplorerEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, private val index: Int
): FlowLayout(horizontalSizing, verticalSizing, Algorithm.HORIZONTAL) {

    init {
        mouseEnter().subscribe {
            cursorStyle(CursorStyle.HAND)
        }
    }

    abstract val icon: Identifier
    abstract fun buildContextButtons(): List<Component>

    var isFocused = false

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
        if (isFocused) this.drawFocusHighlight(context, mouseX, mouseY, partialTicks, delta)
    }

    override fun onMouseDown(click: Click, doubled: Boolean): Boolean {
        if (doubled && FileBrowserHandler.handleDirectoryClick(index)) {
            return true
        }

        isFocused = !isFocused

        parent?.children()?.filterIsInstance<FileExplorerEntryComponent>()?.forEach {
            if (it != this) {
                it.isFocused = false
            }
        }

        if (isFocused) {
            FileBrowser.INSTANCE.updateButtons(this)
        } else {
            FileBrowser.INSTANCE.updateButtons(null)
        }

        return super.onMouseDown(click, doubled)
    }
}