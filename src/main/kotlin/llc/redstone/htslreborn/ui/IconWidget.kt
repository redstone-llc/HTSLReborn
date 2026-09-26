package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.utils.CursorManager
import llc.redstone.htslreborn.utils.TextUtils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

abstract class IconWidget(x: Int, y: Int, w: Int, h: Int, comp: Component) : AbstractWidget(x, y, w, h, comp) {
    abstract val icons: List<Icon>
    abstract val BACKGROUND: Identifier
    open val isWholeHovered: Boolean = false

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        for (icon in icons) {
            if (icon.isHovered(mouseButtonEvent.x.toInt() - x, mouseButtonEvent.y.toInt() - y)) {
                icon.run()
                return true
            }
        }

        return false
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (isWholeHovered && isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
            CursorManager.setHandCursor()
        }
        this.renderBackground(guiGraphics, mouseX, mouseY, a)

        val relativeMouseX = mouseX - x
        val relativeMouseY = mouseY - y

        for (icon in icons) {
            icon.draw(guiGraphics, x + icon.x, y + icon.y)

            val iconHovered = icon.isHovered(relativeMouseX, relativeMouseY)
            if (iconHovered) {
                icon.tooltip?.let { tooltip ->
                    guiGraphics.setComponentTooltipForNextFrame(
                        MC.font,
                        TextUtils.legacyTooltip(tooltip.string),
                        mouseX,
                        mouseY
                    )
                }
            }
            if (!icon.disabled && iconHovered) {
                CursorManager.setHandCursor()
                guiGraphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    BACKGROUND,
                    x + icon.x,
                    y + icon.y,
                    icon.x.toFloat(),
                    0.0f,
                    icon.width,
                    icon.height,
                    width,
                    height,
                    0xFFBFBFCC.toInt()
                )
                icon.draw(guiGraphics, x + icon.x, y + icon.y, 0xFFBFBFCC.toInt())
            }
        }
    }

    private fun renderBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
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