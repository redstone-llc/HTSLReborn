package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.core.OwoUIGraphics
import io.wispforest.owo.ui.core.Positioning
import io.wispforest.owo.ui.core.Sizing
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.systemsapi.importer.ActionContainer
import net.minecraft.client.gui.Click

class DropdownComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing
) : FlowLayout(horizontalSizing, verticalSizing, Algorithm.VERTICAL) {
    var isVisible = false
    var x = 0
    var y = 0

    fun handleDropdownButton(buttonComponent: ButtonComponent?) {
        isVisible = !isVisible
        x = buttonComponent!!.x() + 5
        y = buttonComponent.y() + 5 - this.height()
        positioning(Positioning.absolute(x, y))
    }

    override fun draw(graphics: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        if (!isVisible) return
        super.draw(graphics, mouseX, mouseY, partialTicks, delta)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing): DropdownComponent {
            return DropdownComponent(horizontalSizing, verticalSizing)
        }

        fun click(button: ButtonComponent) {
            if (FileExplorer.INSTANCE.focus !is ScriptEntryComponent) return
            val method = when (button.id()) {
                "add" -> ActionContainer::addActions
                "replace" -> ActionContainer::setActions
                "update" -> ActionContainer::updateActions
                else -> throw IllegalStateException("Unknown import type: ${button.id()}")
            }
            val file = (FileExplorer.INSTANCE.focus as ScriptEntryComponent).file

            FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.IMPORT, file.name)
            HTSLImporter.importFile(file, method) {
                FileExplorer.INSTANCE.hideWorkingScreen()
            }
            FileExplorer.INSTANCE.dropdown.isVisible = false
        }
    }
}