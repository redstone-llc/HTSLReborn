package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.CursorStyle
import io.wispforest.owo.ui.core.OwoUIDrawContext
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.ui.FileBrowser
import llc.redstone.htslreborn.ui.FileBrowserHandler
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.ui.FileHandler.htslExtensions
import llc.redstone.htslreborn.ui.FileHandler.itemExtensions
import net.minecraft.client.gui.Click
import net.minecraft.util.Identifier
import java.io.File

class FileEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, private val index: Int
): FlowLayout(horizontalSizing, verticalSizing, Algorithm.HORIZONTAL) {
    var isFocused = false

    var file: File? = FileHandler.filteredFiles.getOrNull(index)?.let {
        FileHandler.getFile(it)
    }

    private val folderIdent = Identifier.of("htslreborn", "textures/ui/file_browser/placeholder.png")
    private val itemIdent = Identifier.of("htslreborn", "textures/ui/file_browser/placeholder.png")
    private val scriptIdent = Identifier.of("htslreborn", "textures/ui/file_browser/placeholder.png")
    private val unknownIdent = Identifier.of("htslreborn", "textures/ui/file_browser/placeholder.png")

    init {
        mouseEnter().subscribe {
            cursorStyle(CursorStyle.HAND)
        }
    }

    fun spriteId(): Identifier {
        return when {
            isFolder() -> folderIdent
            isItem() -> itemIdent
            isHTSLScript() -> scriptIdent
            else -> unknownIdent
        }
    }

    fun isFolder(): Boolean {
        return file?.isDirectory == true
    }

    fun isItem(): Boolean {
        val fileName = file?.name?.lowercase() ?: return false
        return itemExtensions.any { fileName.endsWith(it) }
    }

    fun isHTSLScript(): Boolean {
        val fileName = file?.name?.lowercase() ?: return false
        return htslExtensions.any { fileName.endsWith(it) }
    }

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

        parent?.children()?.filterIsInstance<FileEntryComponent>()?.forEach {
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

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int): FileEntryComponent {
            return FileEntryComponent(horizontalSizing, verticalSizing, index)
        }
    }
}