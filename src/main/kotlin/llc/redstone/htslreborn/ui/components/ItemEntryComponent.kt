package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.utils.ItemConvertUtils
import llc.redstone.systemsapi.util.ItemStackUtils.giveItem
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.io.File

class ItemEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int, val file: File
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.of("htslreborn", "textures/ui/file_explorer/item_icon.png")

    override fun buildContextButtons(): List<UIComponent> {
        val give = UIComponents.button(Text.translatable("htslreborn.explorer.button.item.give"), {
            val item = FileHandler.getItemForFile(file) ?: return@button
            val slot = convertSlot(MC.player?.inventory?.emptySlot ?: -1)
            item.giveItem(slot)
        }).apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.item.give.description")))
        }

        val save = UIComponents.button(Text.translatable("htslreborn.explorer.button.item.save"), {
            val item = MC.player?.inventory?.selectedStack ?: return@button
            ItemConvertUtils.itemStackToFile(item, file)
        }).apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.item.save.description")))
        }

        val spacer = UIComponents.spacer()

        val open = UIComponents.button(Text.of("✎"), {
            Util.getOperatingSystem().open(file)
        }).apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.item.open.description")))
        }

        val delete = UIComponents.button(Text.of("\uD83D\uDDD1"), {
            file.delete()
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        }).apply {
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

    private fun convertSlot(slot: Int) = when (slot) {
        in 0..8 -> slot + 36
        in 9..35 -> slot
        else -> -1
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int, file: File): ExplorerEntryComponent {
            return ItemEntryComponent(horizontalSizing, verticalSizing, index, file)
        }
    }

}