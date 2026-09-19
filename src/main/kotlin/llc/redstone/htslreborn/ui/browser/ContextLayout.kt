package llc.redstone.htslreborn.ui.browser

import llc.redstone.htslreborn.data.ContextTarget
import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.utils.MenuUtils
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.layouts.AbstractLayout
import net.minecraft.client.gui.layouts.LayoutElement
import java.util.function.Consumer

class ContextLayout(
    x: Int,
    y: Int,
    width: Int,
    height: Int
) : AbstractLayout(x, y, width, height) {
    private val widgets = linkedMapOf<ContextEntry, AbstractWidget>()

    data class ContextEntry(val context: ImportContext?, val target: ContextTarget?)

    private fun visiblePaths(): List<ContextEntry> {
        val entries = if (BrowsingWidget.openContext != null) {
            BrowsingWidget.cachedContexts[BrowsingWidget.openContext!!]?.map {
                ContextEntry(
                    BrowsingWidget.openContext,
                    it.target
                )
            } ?: emptyList()
        } else {
            ImportContext.entries.mapNotNull {
                if (!MenuUtils.isActionContainerOpen() && it == ImportContext.DEFAULT) null else ContextEntry(it, null)
            }
        }
        return entries.filter { entry ->
            val name = entry.target?.name
                ?: entry.context?.name
                ?: "Default"
            FileHandler.matchesSearch(name)
        }
    }

    private fun sync() {
        val paths = visiblePaths()
        val keep = paths.toSet()
        widgets.keys.retainAll(keep)
        for (path in paths) {
            widgets.getOrPut(path) {
                if (path.context == ImportContext.DEFAULT) {
                    return@getOrPut ScriptWidget(scriptContainer = ScriptContainer(ImportContext.DEFAULT))
                }
                if (BrowsingWidget.openContext == null) FolderWidget(context = path.context) else ScriptWidget(
                    scriptContainer = ScriptContainer(
                        context = path.context ?: BrowsingWidget.openContext!!,
                        target = path.target!!
                    )
                )
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
