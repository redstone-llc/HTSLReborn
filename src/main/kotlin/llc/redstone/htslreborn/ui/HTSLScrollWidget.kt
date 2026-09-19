package llc.redstone.htslreborn.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractContainerWidget
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.layouts.Layout
import net.minecraft.client.gui.narration.NarratableEntry
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class HTSLScrollWidget(
    x: Int, y: Int, width: Int, height: Int,
    val layout: Layout, scrollAmount: Double
) :
    AbstractContainerWidget(x, y, width, height, Component.empty(), defaultSettings(17)) {
    companion object {
        const val BAR_WIDTH = 3

        val SCROLLER = Identifier.withDefaultNamespace("widget/scroller")
        val SCROLLER_BACKGROUND = Identifier.withDefaultNamespace("widget/scroller_background")
    }

    private val childWidgets = mutableListOf<AbstractWidget>()

    init {
        setScrollAmount(scrollAmount)
    }

    private fun syncChildren() {
        childWidgets.clear()
        layout.visitWidgets { childWidgets.add(it) }
    }

    override fun contentHeight(): Int = layout.height

    override fun scrollRate(): Double = 17.0

    override fun scrollBarX(): Int = this.x + this.width - BAR_WIDTH

    override fun isOverScrollbar(mouseX: Double, mouseY: Double): Boolean {
        return mouseX >= scrollBarX() && mouseX < scrollBarX() + BAR_WIDTH &&
                mouseY >= y && mouseY < y + height
    }

    override fun extractScrollbar(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
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

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        layout.setPosition(x, y - scrollAmount().toInt())
        layout.arrangeElements()
        syncChildren()
        guiGraphics.enableScissor(x, y, x + width, y + height)
        for (child in childWidgets) {
            child.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        }
        guiGraphics.disableScissor()
        extractScrollbar(guiGraphics, mouseX, mouseY)
    }

    override fun children(): MutableList<out GuiEventListener> = childWidgets

    override fun getNarratables(): Collection<NarratableEntry> = childWidgets

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
    }
}
