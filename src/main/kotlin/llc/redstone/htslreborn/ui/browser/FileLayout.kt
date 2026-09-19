package llc.redstone.htslreborn.ui.browser

import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.layouts.AbstractLayout
import net.minecraft.client.gui.layouts.LayoutElement
import java.nio.file.Path
import java.util.function.Consumer
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class FileLayout(
    x: Int,
    y: Int,
    width: Int,
    height: Int
) : AbstractLayout(x, y, width, height) {
    private val widgets = linkedMapOf<Path, AbstractWidget>()

    private fun visiblePaths(): List<Path> {
        return FileHandler.filteredFiles.filter { path ->
            path.isDirectory() || path.name.endsWith(".htsl", ignoreCase = true)
        }
    }

    private fun sync() {
        val paths = visiblePaths()
        val keep = paths.toSet()
        widgets.keys.retainAll(keep)
        for (path in paths) {
            widgets.getOrPut(path) {
                if (path.isDirectory()) FolderWidget(path) else ScriptWidget(path)
            }
        }
    }

    override fun visitChildren(consumer: Consumer<LayoutElement>) {
        sync()
        for (widget in widgets.values) {
            consumer.accept(widget)
        }
    }

    override fun arrangeElements() {
        super.arrangeElements()
        sync()
        var childY = y
        for (widget in widgets.values) {
            widget.setPosition(x, childY)
            childY += widget.height + 1
        }
    }

    override fun getWidth(): Int = 200

    override fun getHeight(): Int {
        sync()
        return widgets.size * 17
    }
}
