package llc.redstone.htslreborn.ui

import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

data class Icon(
    val x: Int,
    val y: Int,
    val width: Int = 15,
    val height: Int = 16,
    val disabled: Boolean = false,
    val texture: Identifier? = null,
    val textureWidth: Int = 7,
    val textureHeight: Int = 7,
    val tooltip: Component? = null,
    val run: () -> Unit,
) {
    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in x..<x + width && mouseY in y..<y + height
    }

    fun draw(guiGraphics: GuiGraphicsExtractor, x: Int, y: Int, color: Int = 0xFFFFFFFF.toInt()) {
        if (texture == null) return
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x + 4,
            y + 4,
            0.0f,
            0.0f,
            textureWidth,
            textureHeight,
            textureWidth,
            textureHeight,
            color
        )
    }
}