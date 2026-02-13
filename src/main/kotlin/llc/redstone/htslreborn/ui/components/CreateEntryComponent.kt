package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.UIComponents
import io.wispforest.owo.ui.core.Sizing
import io.wispforest.owo.ui.core.UIComponent
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.exportingFile
import llc.redstone.htslreborn.htslio.HTSLExporter
import llc.redstone.htslreborn.ui.FileExplorer
import llc.redstone.htslreborn.ui.FileHandler
import llc.redstone.htslreborn.ui.FileHandler.search
import llc.redstone.htslreborn.utils.ItemUtils.saveItem
import llc.redstone.htslreborn.utils.UIErrorToast
import net.minecraft.client.gui.Click
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.name

class CreateEntryComponent(
    horizontalSizing: Sizing, verticalSizing: Sizing, override val index: Int
) : ExplorerEntryComponent(horizontalSizing, verticalSizing, index) {
    override val icon: Identifier = Identifier.of("htslreborn", "textures/ui/file_explorer/create_icon.png")

    @OptIn(ExperimentalPathApi::class)
    override fun buildContextButtons(): List<UIComponent> {
        val exportScript = UIComponents.button(Text.translatable("htslreborn.explorer.button.create.script")) {
            handleScriptExport()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.create.script.description")))
        }

        val exportItem = UIComponents.button(Text.translatable("htslreborn.explorer.button.create.item")) {
            handleItemExport()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.create.item.description")))
        }

        val createFolder = UIComponents.button(Text.translatable("htslreborn.explorer.button.create.folder")) {
            handleFolderCreate()
        }.apply {
            sizing(Sizing.content(), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.create.folder.description")))
        }

        return listOf(
            exportScript,
            exportItem,
            UIComponents.spacer(),
            createFolder
        )
    }

    fun handleScriptExport() {
        val path = FileHandler.currentDir.resolve("$search.htsl")
        FileExplorer.INSTANCE.showWorkingScreen(FileExplorer.WorkingScreenType.EXPORT, path.name)
        exportingFile = path
        HTSLExporter.exportFile(path) {
            FileExplorer.INSTANCE.hideWorkingScreen()
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        }
    }

    fun handleItemExport() {
        val path = FileHandler.currentDir.resolve("$search.nbt")
        MC.player?.saveItem(path)
        FileHandler.refreshFiles()
        FileExplorer.INSTANCE.refreshExplorer()
    }

    fun handleFolderCreate() {
        val path = FileHandler.currentDir.resolve(search)
        if (!path.exists()) {
            path.createDirectories()
            FileHandler.refreshFiles()
            FileExplorer.INSTANCE.refreshExplorer()
        } else {
            UIErrorToast.report("${path.name} already exists")
        }
    }

    override fun onMouseDown(click: Click, doubled: Boolean): Boolean {
        return super.onMouseDown(click, false)
    }

    companion object {
        fun create(horizontalSizing: Sizing, verticalSizing: Sizing): ExplorerEntryComponent {
            return CreateEntryComponent(horizontalSizing, verticalSizing, -1)
        }
    }

}