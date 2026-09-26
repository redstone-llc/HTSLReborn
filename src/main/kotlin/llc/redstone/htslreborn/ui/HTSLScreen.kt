package llc.redstone.htslreborn.ui

//? if >=26.2 {
/*import llc.redstone.htslreborn.screen
*///?}

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.accessor.HandledScreenAccessor
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.ui.browser.BrowsingWidget
import llc.redstone.htslreborn.ui.browser.FileHandler
import llc.redstone.htslreborn.ui.working.BottombarWidget
import llc.redstone.htslreborn.ui.working.WorkingWidget
import llc.redstone.htslreborn.utils.CursorManager
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.util.concurrent.ConcurrentLinkedDeque

class HTSLScreen : Screen(Component.translatable("htslreborn.screen.working")) {
    companion object {
        @JvmStatic
        var INSTANCE = HTSLScreen()

        val housingTitles = listOf(
            "Housing Menu",
            "Systems",
            "Actions",
            "Functions",
            "Regions",
            "Commands",
            "Custom Menus",
            "Edit Menu",
            "NPCs",
            "Edit NPC"
        )

        @JvmStatic
        fun shouldBeVisible(): Boolean {
            return Queue.isActive || housingTitles.any { title -> MC.screen?.title?.string?.contains(title) == true } || MC.screen is InventoryScreen/* || MC.screen is CreativeModeInventoryScreen*/
        }

        var isBrowsing = false
        var browsingType: BrowsingType? = BrowsingType.IMPORT_LEFT

        fun import() {
            if (isBrowsing && (browsingType == BrowsingType.EXPORT_RIGHT || browsingType == BrowsingType.EXPORT_LEFT)) {
                browsingType = BrowsingType.IMPORT_LEFT
                FileHandler.refreshFiles()
                return
            }
            isBrowsing = !isBrowsing
            if (isBrowsing) {
                browsingType = BrowsingType.IMPORT_LEFT
                FileHandler.refreshFiles()
            }
        }

        fun export() {
            if (isBrowsing && (browsingType == BrowsingType.IMPORT_RIGHT || browsingType == BrowsingType.IMPORT_LEFT)) {
                browsingType = BrowsingType.EXPORT_LEFT
                FileHandler.refreshFiles()
                return
            }
            isBrowsing = !isBrowsing
            if (isBrowsing) {
                browsingType = BrowsingType.EXPORT_LEFT
                FileHandler.refreshFiles()
            }
        }

        fun notBrowsing() {
            isBrowsing = false
            browsingType = null
            BrowsingWidget.clearSelection()
        }
    }

    val imageWidth = 176
    val biggerImageWidth = 223

    val widgets = ConcurrentLinkedDeque<AbstractWidget>()

    override fun init() {
        super.init()
        val accessor = MC.screen as? HandledScreenAccessor ?: return
        val menuSize = getMenuHeight()
        val menuTop = if (Queue.isActive || isBrowsing) (this.height - menuSize) / 2 else accessor.getGuiTop()

        if (isBrowsing && widgets.find { it is BrowsingWidget } == null) {
            widgets.clear()
            widgets.add(TopbarWidget((this.width - biggerImageWidth) / 2, menuTop - 19, wide = true, containersActive = !Queue.isActive))
            widgets.add(BrowsingWidget((this.width - biggerImageWidth) / 2, (this.height - 222) / 2))
            if (Queue.isActive) {
                widgets.add(BottombarWidget((this.width - biggerImageWidth) / 2, (this.height - menuSize) / 2 + menuSize))
            }
            return
        }
        if (Queue.isActive && !isBrowsing && widgets.find { it is WorkingWidget } == null) {
            widgets.clear()
            widgets.add(TopbarWidget((this.width - biggerImageWidth) / 2, menuTop - 19, wide = true, containersActive = !Queue.isActive))
            widgets.add(WorkingWidget((this.width - biggerImageWidth) / 2, (this.height - 222) / 2))
            widgets.add(BottombarWidget((this.width - biggerImageWidth) / 2, (this.height - menuSize) / 2 + menuSize))
            return
        }

        if (!Queue.isActive && !isBrowsing) {
            if (widgets.size > 1) widgets.clear()

            widgets.add(TopbarWidget((this.width - imageWidth) / 2, menuTop - 19))
        }
    }

    override fun extractRenderState(guiGraphics: GuiGraphicsExtractor, i: Int, j: Int, f: Float) {
        super.extractRenderState(guiGraphics, i, j, f)
        init()
        widgets.forEach { widget ->
            widget.extractRenderState(guiGraphics, i, j, f)
        }
        CursorManager.resetCursor()
    }

    override fun onClose() {
        notBrowsing()
        CursorManager.resetCursor()
        super.onClose()
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        widgets.forEach { widget ->
            if (widget.mouseClicked(mouseButtonEvent, bl)) {
                return true
            }
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        widgets.forEach { widget ->
            if (widget.mouseScrolled(d, e, f, g)) {
                return true
            }
        }
        return super.mouseScrolled(d, e, f, g)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        widgets.forEach { widget ->
            if (widget.mouseDragged(mouseButtonEvent, d, e)) {
                return true
            }
        }
        return super.mouseDragged(mouseButtonEvent, d, e)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        widgets.forEach { widget ->
            if (widget.mouseReleased(mouseButtonEvent)) {
                return true
            }
        }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        widgets.forEach { widget ->
            if (widget.keyPressed(keyEvent)) {
                return true
            }
        }
        return super.keyPressed(keyEvent)
    }

    override fun charTyped(characterEvent: CharacterEvent): Boolean {
        widgets.forEach { widget ->
            if (widget.charTyped(characterEvent)) {
                return true
            }
        }
        return super.charTyped(characterEvent)
    }

    fun getMenuHeight(): Int {
        if (Queue.isActive || isBrowsing) return 222
        return 144
    }

    enum class BrowsingType {
        IMPORT_LEFT,
        IMPORT_RIGHT,
        EXPORT_LEFT,
        EXPORT_RIGHT
    }
}