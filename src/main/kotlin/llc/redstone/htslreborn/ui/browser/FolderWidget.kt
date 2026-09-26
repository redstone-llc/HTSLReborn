package llc.redstone.htslreborn.ui.browser

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.config.HTSLConfig
import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.ui.Icon
import llc.redstone.htslreborn.ui.IconWidget
import llc.redstone.htslreborn.ui.browser.FileExplorerHandler.setWatchedDir
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

class FolderWidget(val file: Path? = null, val context: ImportContext? = null) : IconWidget(0, 0, 200, 15, Component.translatable("htslreborn.browser.folder")) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/browser/folder.png")

        val DELETE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/delete.png")
        val OPEN_EXTERNALLY = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/open_externally.png")
    }

    var hovered = false

    val hoveredIcons = listOf(
        Icon(155, 0, texture = DELETE, tooltip = Component.translatable("htslreborn.browser.delete")) {
            val target = file ?: return@Icon
            HTSLConfig.requestDelete(target)
        },
        Icon(170, 0, texture = OPEN_EXTERNALLY, tooltip = Component.translatable("htslreborn.browser.open_externally")) {
            if (file == null) return@Icon
            // Open Externally
            //? if >=26.3 {
            /*Blaze3D.openPath(file)
            *///?} else {
            Util.getPlatform().openPath(file)
            //?}
        },
        Icon(185, 0, tooltip = Component.translatable("htslreborn.browser.open")) {
            if (file == null) return@Icon
            // Open Folder
            FileHandler.currentDir = file
            FileHandler.refreshFiles()
            setWatchedDir(file)
            BrowsingWidget.fileScroll?.setScrollAmount(0.0)
        }
    )

    val contextIcons = listOf(
        Icon(185, 0, height = 15, tooltip = Component.translatable("htslreborn.browser.open")) {
            if (context == null) return@Icon
            BrowsingWidget.openContext = context
            BrowsingWidget.contextScroll?.setScrollAmount(0.0)
        }
    )

    override val icons: List<Icon>
        get() = if (hovered) if (file != null) hoveredIcons else contextIcons else emptyList()
    override val BACKGROUND: Identifier
        get() = FolderWidget.BACKGROUND
    override val isWholeHovered: Boolean = true

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, delta)
        val label = when {
            file != null -> Component.literal(file.name)
            context != null -> Component.translatable(context.translationKey())
            else -> Component.translatable("htslreborn.browser.unknown_folder")
        }
        guiGraphics.drawEllipsis(
            MC.font,
            label,
            x + 14,
            y + 4,
            if (hovered) 138 else 173,
            0xFF3F3F3F.toInt(),
            false
        )
        hovered = mouseX in x..(x + width) && mouseY in y..(y + height)
    }

    override fun mouseClicked(mouseButtonEvent: MouseButtonEvent, bl: Boolean): Boolean {
        if (hovered) {
            if (file != null) {
                FileHandler.currentDir = file
                FileHandler.refreshFiles()
                setWatchedDir(file)
                BrowsingWidget.fileScroll?.setScrollAmount(0.0)
            } else if (context != null) {
                BrowsingWidget.openContext = context
                BrowsingWidget.contextScroll?.setScrollAmount(0.0)
            }
            return true
        }
        return super.mouseClicked(mouseButtonEvent, bl)
    }
}