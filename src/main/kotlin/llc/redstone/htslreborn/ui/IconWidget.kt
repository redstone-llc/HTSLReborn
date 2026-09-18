package llc.redstone.htslreborn.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

abstract class IconWidget(x: Int, y: Int, w: Int, h: Int, comp: Component) : AbstractWidget(x, y, w, h, comp) {
    abstract val icons: List<Icon>
    abstract val BACKGROUND: Identifier

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        for (icon in icons) {
            if (icon.isHovered(mouseButtonEvent.x.toInt() - x, mouseButtonEvent.y.toInt() - y)) {
                icon.run()
                return true
            }
        }

        return super.mouseClicked(mouseButtonEvent, bl)
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta)

        val relativeMouseX = mouseX - x
        val relativeMouseY = mouseY - y

        for (icon in icons) {
            if (!icon.disabled && icon.isHovered(relativeMouseX, relativeMouseY)) {
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    BACKGROUND,
                    x + icon.x,
                    y + icon.y,
                    icon.x.toFloat(),
                    0.0f,
                    15,
                    16,
                    width,
                    height,
                    0xFFBFBFCC.toInt()
                )
            }
        }
    }

    private fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            x,
            y,
            0.0f,
            0.0f,
            width,
            height,
            width,
            height
        )
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
    }
}