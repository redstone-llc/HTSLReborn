package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.queue.Queue
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class HTSLScrollWidget(val layout: Layout, scrollAmount: Double) :
    AbstractContainerWidget(layout.x, layout.y, layout.width, layout.height, Component.empty()) {
    companion object {
        const val BAR_WIDTH = 3

        val SCROLLER = Identifier.withDefaultNamespace("widget/scroller")
        val SCROLLER_BACKGROUND = Identifier.withDefaultNamespace("widget/scroller_background")
    }

    init {
        setScrollAmount(scrollAmount)
    }

    override fun contentHeight(): Int = layout.height

    override fun scrollRate(): Double = 17.0

    override fun scrollbarVisible(): Boolean = true;

    override fun scrollBarX(): Int = this.x + this.width - BAR_WIDTH

    override fun isOverScrollbar(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= scrollBarX() && mouseX < scrollBarX() + BAR_WIDTH &&
                mouseY >= y && mouseY < y + height
    }

    override fun renderScrollbar(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        if (this.maxScrollAmount() <= 0) {
            guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SCROLLER,
                scrollBarX(),
                y,
                BAR_WIDTH,
                height
            )
            return
        }
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            SCROLLER_BACKGROUND,
            scrollBarX(),
            y,
            BAR_WIDTH,
            height
        )
        guiGraphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            SCROLLER,
            scrollBarX(),
            scrollBarY(),
            BAR_WIDTH,
            scrollerHeight()
        )
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        layout.setPosition(x, y - scrollAmount().toInt())
        layout.arrangeElements()
        guiGraphics.enableScissor(x, y, x + width, y + height)
        for (entry in Queue.containers) {
            entry.render(guiGraphics, mouseX, mouseY, delta)
        }
        guiGraphics.disableScissor()
        renderScrollbar(guiGraphics, mouseX, mouseY)
    }

    override fun children(): MutableList<out GuiEventListener> = Queue.containers

    override fun getNarratables(): Collection<NarratableEntry> = Queue.containers

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
    }
}