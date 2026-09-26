package llc.redstone.htslreborn.ui.browser

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.ui.Icon
import llc.redstone.htslreborn.ui.IconWidget
import llc.redstone.htslreborn.utils.TextUtils
import llc.redstone.htslreborn.utils.TextUtils.drawEllipsis
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
//? if >=26.3 {
/*import com.mojang.blaze3d.Blaze3D
*///?} else {
import net.minecraft.util.Util
//?}
import java.nio.file.Path
import kotlin.io.path.name

class ScriptWidget(val file: Path? = null, val scriptContainer: ScriptContainer? = null) : IconWidget(0, 0, 200, 15, Component.translatable("htslreborn.browser.script")) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/browser/script.png")

        val DELETE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/delete.png")
        val OPEN_EXTERNALLY = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/open_externally.png")
    }

    var hovered = false

    val hoveredIcons = listOf(
        Icon(170, 0, texture = DELETE) {
            if (file == null) return@Icon
            // Delete file
        },
        Icon(185, 0, texture = OPEN_EXTERNALLY) {
            if (file == null) return@Icon
            //? if >=26.3 {
            /*Blaze3D.openPath(file)
            *///?} else {
            Util.getPlatform().openPath(file)
            //?}
        }
    )

    override val icons: List<Icon>
        get() = if (hovered && file != null) hoveredIcons else emptyList()
    override val BACKGROUND: Identifier
        get() = ScriptWidget.BACKGROUND
    override val isWholeHovered: Boolean = true

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.drawEllipsis(
            MC.font,
            entryLabel(),
            x + 14,
            y + 4,
            if (hovered) if (file != null) 160 else 190 else 190,
            0xFF3F3F3F.toInt(),
            false
        )
        hovered = mouseX in x..(x + width) && mouseY in y..(y + height)
    }

    private fun entryLabel(): String {
        file?.name?.let { return it }
        scriptContainer?.target?.name?.let { return TextUtils.titleCase(it) }
        return TextUtils.translate("htslreborn.browser.default")
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (super.mouseClicked(mouseButtonEvent, bl)) return true
        if (hovered) {
            BrowsingWidget.fillSearch(entryLabel())
            return true
        }
        return false
    }
}