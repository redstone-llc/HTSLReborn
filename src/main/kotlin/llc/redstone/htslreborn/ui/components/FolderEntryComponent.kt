package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class FolderEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, private val index: Int
) : FileExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.of("htslreborn", "textures/ui/file_browser/script_icon.png")

    override fun buildContextButtons(): List<Component> {
        val spacer = Components.spacer()
        val delete = Components.button(Text.of("\uD83D\uDDD1")) { /*...*/ }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.folder.delete.description")))
        }
        val open = Components.button(Text.of("✎")) { /*...*/ }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.folder.open.description")))
        }

        return listOf(
            spacer,
            open,
            delete
        )
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int): FileExplorerEntryComponent {
            return FolderEntryComponent(horizontalSizing, verticalSizing, index)
        }
    }

}