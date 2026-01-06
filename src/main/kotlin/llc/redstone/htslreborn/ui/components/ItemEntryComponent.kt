package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.ui.FileExplorerHandler
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import java.io.File

class ItemEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int, val file: File
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.of("htslreborn", "textures/ui/file_explorer/item_icon.png")

    override fun buildContextButtons(): List<Component> {
        val give = Components.button(Text.translatable("htslreborn.explorer.button.item.give"), FileExplorerHandler::onActionClicked).apply {
            id("giveItem")
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.item.give.description")))
        }
        val save = Components.button(Text.translatable("htslreborn.explorer.button.item.save"), FileExplorerHandler::onActionClicked).apply {
            id("saveItem")
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.item.save.description")))
        }
        val spacer = Components.spacer()
        val open = Components.button(Text.of("✎"), FileExplorerHandler::onActionClicked).apply {
            id("open")
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.item.open.description")))
        }
        val delete = Components.button(Text.of("\uD83D\uDDD1"), FileExplorerHandler::onActionClicked).apply {
            id("delete")
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.item.delete.description")))
        }

        return listOf(
            give,
            save,
            spacer,
            open,
            delete
        )
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int, file: File): ExplorerEntryComponent {
            return ItemEntryComponent(horizontalSizing, verticalSizing, index, file)
        }
    }

}