package llc.redstone.htslreborn.ui

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier

data class Icon(val x: Int, val y: Int, val width: Int = 15, val height: Int = 15, val disabled: Boolean = false, val texture: Identifier? = null, val run: () -> Unit) {
    fun isHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX in x..<x + width && mouseY in y..<y + height
    }

    fun draw(guiGraphics: GuiGraphics, x: Int, y: Int, color: Int = 0xFFFFFFFF.toInt()) {
        if (texture == null) return
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x + 4,
            y + 4,
            0.0f,
            0.0f,
            7,
            7,
            7,
            7,
            color
        )
    }
}