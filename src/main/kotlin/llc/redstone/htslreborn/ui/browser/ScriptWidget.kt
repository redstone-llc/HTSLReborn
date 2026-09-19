package llc.redstone.htslreborn.ui.browser

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.ui.Icon
import llc.redstone.htslreborn.ui.IconWidget
import llc.redstone.htslreborn.utils.TextUtils
import llc.redstone.htslreborn.utils.TextUtils.drawEllipsis
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.util.Util
import java.nio.file.Path
import kotlin.io.path.name

class ScriptWidget(val file: Path? = null, val scriptContainer: ScriptContainer? = null) : IconWidget(0, 0, 200, 15, Component.literal("Script")) {
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
            Util.getPlatform().openPath(file)
        }
    )

    override val icons: List<Icon>
        get() = if (hovered && file != null) hoveredIcons else emptyList()
    override val BACKGROUND: Identifier
        get() = ScriptWidget.BACKGROUND

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        super.renderWidget(guiGraphics, mouseX, mouseY, delta)
        guiGraphics.drawEllipsis(
            MC.font,
            file?.name ?: scriptContainer?.target?.name?.let { TextUtils.titleCase(it) } ?: "Default",
            x + 14,
            y + 4,
            if (hovered) if (file != null) 160 else 190 else 190,
            0xFF3F3F3F.toInt(),
            false
        )
        hovered = mouseX in x..(x + width) && mouseY in y..(y + height)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (hovered && mouseButtonEvent.x.toInt() in x..(x + 170) && mouseButtonEvent.y.toInt() in y..(y + height)) {
            if (file != null) {
                BrowsingWidget.filePath = file
            } else if (scriptContainer != null) {
                BrowsingWidget.container = scriptContainer
            }
            return true
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }
}