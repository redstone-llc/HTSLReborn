package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.core.Component
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.ui.FileBrowser
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Identifier

class ScriptEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, private val index: Int,
) : FileExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.of("htslreborn", "textures/ui/file_browser/script_icon.png")

    override fun buildContextButtons(): List<Component> {
        val import = Containers.horizontalFlow(Sizing.content(), Sizing.fill()).apply {
            children(listOf(
                Components.button(Text.translatable("htslreborn.explorer.button.file.import")) { /*...*/ }.apply {
                    setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.file.import.add.description")))
                },
                Components.button(Text.of("↓")) { FileBrowser.INSTANCE.dropdown.handleDropdownButton(it) }
            ))
        }
        val export = Components.button(Text.translatable("htslreborn.explorer.button.file.export")) { /*...*/ }.apply {
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.file.export.description")))
        }
        val spacer = Components.spacer()
        val open = Components.button(Text.of("✎")) { /*...*/ }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.file.open.description")))
        }
        val delete = Components.button(Text.of("\uD83D\uDDD1")) { /*...*/ }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.file.delete.description")))
        }

        return listOf(
            import,
            export,
            spacer,
            open,
            delete
        )
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int): FileExplorerEntryComponent {
            return ScriptEntryComponent(horizontalSizing, verticalSizing, index)
        }
    }

}