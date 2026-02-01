package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.container.UIContainers
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import llc.redstone.htslreborn.HTSLReborn.CONFIG
import llc.redstone.htslreborn.HTSLReborn.exportingFile
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.config.HtslConfigModel
import llc.redstone.htslreborn.htslio.HTSLExporter
import llc.redstone.htslreborn.htslio.HTSLImporter
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.systemsapi.importer.ActionContainer
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.Util
import java.io.File

class ScriptEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int, val file: File
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.of("htslreborn", "textures/ui/file_explorer/script_icon.png")
    override fun buildContextButtons(): List<UIComponent> {
        val import = UIContainers.horizontalFlow(Sizing.content(), Sizing.fill()).apply {
            children(
                listOf(
                    UIComponents.button(
                        Text.translatable("htslreborn.explorer.button.script.import")
                    ) {
                        val method = when (CONFIG.defaultImportStrategy) {
                            HtslConfigModel.ImportStrategy.APPEND -> ActionContainer::addActions
                            HtslConfigModel.ImportStrategy.REPLACE -> ActionContainer::setActions
                            HtslConfigModel.ImportStrategy.UPDATE -> ActionContainer::updateActions
                        }
                        FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.IMPORT, file.name)
                        importingFile = file.name
                        HTSLImporter.importFile(file, method) {
                            FileExplorer.INSTANCE.hideWorkingScreen()
                        }
                    }.apply {
                        val tooltipKey = when (CONFIG.defaultImportStrategy) {
                            HtslConfigModel.ImportStrategy.APPEND -> "htslreborn.explorer.button.script.import.append.description"
                            HtslConfigModel.ImportStrategy.REPLACE -> "htslreborn.explorer.button.script.import.replace.description"
                            HtslConfigModel.ImportStrategy.UPDATE -> "htslreborn.explorer.button.script.import.update.description"
                        }
                        setTooltip(Tooltip.of(Text.translatable(tooltipKey)))
                    },
                    UIComponents.button(Text.of("↓")) { FileExplorer.INSTANCE.dropdown.handleDropdownButton(it) }
                ))
        }

        val export = UIComponents.button(Text.translatable("htslreborn.explorer.button.script.export")) {
            FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.EXPORT, file.name)
            exportingFile = file.name
            HTSLExporter.exportFile(file) {
                FileExplorer.INSTANCE.hideWorkingScreen()
            }
        }.apply {
                setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.export.description")))
            }

        val spacer = UIComponents.spacer()

        val open = UIComponents.button(Text.of("✎")) {
            Util.getOperatingSystem().open(file)
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.open.description")))
        }

        val delete = UIComponents.button(Text.of("\uD83D\uDDD1")) {
            file.delete()
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.delete.description")))
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
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing, index: Int, file: File): ExplorerEntryComponent {
            return ScriptEntryComponent(horizontalSizing, verticalSizing, index, file)
        }
    }

}