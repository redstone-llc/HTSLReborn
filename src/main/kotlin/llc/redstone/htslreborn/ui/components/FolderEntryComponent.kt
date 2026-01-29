package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileExplorerHandler.handleDirectoryClick
import llc.redstone.htslreborn.ui.FileHandler
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.io.File

class FolderEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int, val file: File
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.of("htslreborn", "textures/ui/file_explorer/folder_icon.png")

    override fun buildContextButtons(): List<Component> {
        val open = Components.button(Text.translatable("htslreborn.explorer.button.folder.open")
        ) {
            handleDirectoryClick(index)
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.folder.open.description")))
        }

        val spacer = Components.spacer()

        val openExternal = Components.button(Text.of("✎")) {
            Util.getOperatingSystem().open(file)
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.folder.openext.description")))
        }

        val delete = Components.button(Text.of("\uD83D\uDDD1")) {
            file.delete()
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.folder.delete.description")))
        }

        return listOf(
            open,
            spacer,
            openExternal,
            delete
        )
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int, file: File): ExplorerEntryComponent {
            return FolderEntryComponent(horizontalSizing, verticalSizing, index, file)
        }
    }

}