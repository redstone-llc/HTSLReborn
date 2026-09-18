package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.accessor.HandledScreenAccessor
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.ui.working.BottombarWidget
import llc.redstone.htslreborn.ui.working.WorkingTopbarWidget
import llc.redstone.htslreborn.ui.working.WorkingWidget
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.util.concurrent.ConcurrentHashMap

class HTSLScreen : Screen(Component.literal("Working Screen")) {
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
    }

    val imageWidth = 176

    val widgets = ConcurrentHashMap<String, AbstractWidget>()

    override fun init() {
        super.init()
        val accessor = MC.screen as? HandledScreenAccessor ?: return
        val menuSize = getMenuHeight()
        val menuTop = if (Queue.isActive) (this.height - menuSize) / 2 else accessor.getGuiTop()

        if (Queue.isActive && widgets.size <= 1) {
            widgets["topbar"] = WorkingTopbarWidget((this.width - imageWidth) / 2, menuTop - 19)
            widgets["bottombar"] =
                BottombarWidget((this.width - imageWidth) / 2, (this.height - menuSize) / 2 + menuSize)
            widgets["working"] = WorkingWidget((this.width - imageWidth) / 2, (this.height - 222) / 2)
            return
        }

        if (!Queue.isActive) {
            if (widgets.size > 1) widgets.clear()

            widgets["topbar"] = TopbarWidget((this.width - imageWidth) / 2, menuTop - 19)
        }
    }

    override fun render(guiGraphics: GuiGraphics, i: Int, j: Int, f: Float) {
        super.render(guiGraphics, i, j, f)
        init()
        widgets.forEach { (_, widget) ->
            widget.render(guiGraphics, i, j, f)
        }
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        widgets.forEach { (_, widget) ->
            if (widget.mouseClicked(mouseButtonEvent, bl)) {
                return true
            }
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun mouseScrolled(d: Double, e: Double, f: Double, g: Double): Boolean {
        widgets.forEach { (_, widget) ->
            if (widget.mouseScrolled(d, e, f, g)) {
                return true
            }
        }
        return super.mouseScrolled(d, e, f, g)
    }

    override fun mouseDragged(mouseButtonEvent: MouseButtonEvent, d: Double, e: Double): Boolean {
        widgets.forEach { (_, widget) ->
            if (widget.mouseDragged(mouseButtonEvent, d, e)) {
                return true
            }
        }
        return super.mouseDragged(mouseButtonEvent, d, e)
    }

    override fun mouseReleased(mouseButtonEvent: MouseButtonEvent): Boolean {
        widgets.forEach { (_, widget) ->
            if (widget.mouseReleased(mouseButtonEvent)) {
                return true
            }
        }
        return super.mouseReleased(mouseButtonEvent)
    }

    override fun keyPressed(keyEvent: KeyEvent): Boolean {
        widgets.forEach { (_, widget) ->
            if (widget.keyPressed(keyEvent)) {
                return true
            }
        }
        return super.keyPressed(keyEvent)
    }

    fun getMenuHeight(): Int {
        if (Queue.isActive) return 222
        return 144
    }
}