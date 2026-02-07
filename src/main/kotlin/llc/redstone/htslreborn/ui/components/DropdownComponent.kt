package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.OverlayContainer
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Insets
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.Surface
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.systemsapi.importer.ActionContainer
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import kotlin.io.path.name

class DropdownComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing
) : OverlayContainer<FlowLayout>(UIContainers.verticalFlow(horizontalSizing, verticalSizing)) {

    init {
        id("importDropdown")

        child().apply {
            surface(Surface.DARK_PANEL)
            padding(Insets.of(2))
            children(
                listOf(
                    UIComponents.button(Text.translatable("htslreborn.explorer.button.script.import.add")) {
                        click(it)
                    }.apply {
                        id("add")
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.import.add.description")))
                    },
                    UIComponents.button(Text.translatable("htslreborn.explorer.button.script.import.replace")) {
                        click(it)
                    }.apply {
                        id("replace")
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.import.replace.description")))
                    },
//                    UIComponents.button(Text.translatable("htslreborn.explorer.button.script.import.update")) {
//                        click(it)
//                    }.apply {
//                        id("update")
//                        horizontalSizing(Sizing.fill())
//                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
//                        setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.import.update.description")))
//                    },
                )
            )
        }
    }

    companion object {
        fun click(button: ButtonComponent) {
            if (FileExplorer.INSTANCE.focus !is ScriptEntryComponent) return
            val method = when (button.id()) {
                "add" -> ActionContainer::addActions
                "replace" -> ActionContainer::setActions
                "update" -> ActionContainer::updateActions
                else -> throw IllegalStateException("Unknown import type: ${button.id()}")
            }

            val file = (FileExplorer.INSTANCE.focus as ScriptEntryComponent).path
            FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.IMPORT, file.name)
            HTSLImporter.importFile(file, method, onComplete = FileExplorer.INSTANCE::hideWorkingScreen)

            val base = FileExplorer.INSTANCE.base
            val dropdown = base.childById(DropdownComponent::class.java, "importDropdown")
            base.queue { base.removeChild(dropdown) }
        }
    }
}