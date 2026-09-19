package llc.redstone.htslreborn.ui.working

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.queue.Progress
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.ui.Icon
import llc.redstone.htslreborn.ui.IconWidget
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.network.chat.Style
import net.minecraft.resources.Identifier

class BottombarWidget(
    x: Int, y: Int
) : IconWidget(x, y, 223, 17, Component.literal("Bottombar")) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/bottombar/bottombar.png")
        val MCFIVE_FONT = FontDescription.Resource(Identifier.fromNamespaceAndPath("htslreborn", "mc_five"))
        val WORKING_COMPONENT = Component.literal("WORKING...").withStyle(Style.EMPTY.withFont(MCFIVE_FONT))
    }

    override val BACKGROUND: Identifier
        get() = BottombarWidget.BACKGROUND

    override val icons = listOf(
        Icon(208, 1) {
            Queue.clear(true, discardContainers = true)
        }
    )

    init {
//        val menuSize = 114 + ((MC.screen as? ContainerScreen)?.menu?.rowCount ?: 0) * 18
//        startingX = (this.width - this.imageWidth) / 2
//        startingY = (this.height - menuSize) / 2 + menuSize
    }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, delta)

        this.renderRelative(guiGraphics, mouseX, mouseY, delta)
    }

    fun renderRelative(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val progressPixels = width * (Progress.fraction.coerceIn(0f, 1f))

        guiGraphics.text(
            MC.font,
            WORKING_COMPONENT,
            x + 6,
            y + 6,
            0xFF808080.toInt(),
            false
        )

        val component = Progress.getComponent()
            .setStyle(Style.EMPTY.withFont(MCFIVE_FONT))

        val textWidth = MC.font.width(component)

        guiGraphics.text(
            MC.font,
            component,
            x + 203 - textWidth,
            y + 6,
            0xFF808080.toInt(),
            false
        )

        guiGraphics.enableScissor(x, y, x + progressPixels.toInt(), y + 20)
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
            height,
            0xFF69b72b.toInt()
        )
        guiGraphics.text(
            MC.font,
            WORKING_COMPONENT,
            x + 6,
            y + 6,
            0xFF355C16.toInt(),
            false
        )
        guiGraphics.text(
            MC.font,
            component,
            x + 203 - textWidth,
            y + 6,
            0xFF355C16.toInt(),
            false
        )
        guiGraphics.disableScissor()
    }
}