package llc.redstone.htslreborn.ui

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.ButtonComponent
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.*
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.HTSLReborn.importing
import llc.redstone.htslreborn.HTSLReborn.importingFile
import llc.redstone.htslreborn.accessors.HandledScreenAccessor
import llc.redstone.htslreborn.ui.FileExplorerHandler.onSearchChanged
import llc.redstone.htslreborn.ui.FileHandler.filteredFiles
import llc.redstone.htslreborn.ui.FileHandler.htslExtensions
import llc.redstone.htslreborn.ui.FileHandler.itemExtensions
import llc.redstone.htslreborn.ui.FileHandler.refreshFiles
import llc.redstone.htslreborn.ui.components.*
import llc.redstone.htslreborn.ui.components.TimeRemainingComponent
import llc.redstone.systemsapi.SystemsAPI
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.client.resource.language.I18n
import net.minecraft.text.Text
import net.minecraft.util.Util
import java.io.File

class FileExplorer() : BaseOwoScreen<FlowLayout>() {
    companion object {
        @JvmStatic
        var INSTANCE = FileExplorer()

        @JvmStatic
        fun inActionGui(): Boolean {
            if (importing) return true
            val screen = MC.currentScreen as? GenericContainerScreen ?: return false
            val title = screen.title.string
            return title.contains(Regex(I18n.translate("htslreborn.action.container.title")))
        }
    }

    var focus: ExplorerEntryComponent? = null

    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    private fun buildTitle(): Component {
        return Components.label(Text.translatable("htslreborn.explorer.title")).apply {
            sizing(Sizing.fill(), Sizing.content())
            horizontalTextAlignment(HorizontalAlignment.CENTER)
            verticalTextAlignment(VerticalAlignment.CENTER)
            margins(Insets.of(2))
        }
    }

    private val searchBox = Components.textBox(Sizing.expand()).apply {
        verticalSizing(Sizing.fill())
        setPlaceholder(Text.translatable("htslreborn.explorer.search"))
    }

    override fun charTyped(input: CharInput): Boolean {
        if (searchBox.isFocused) {
            return searchBox.charTyped(input).also {
                onSearchChanged(searchBox.text)
            }
        }
        return super.charTyped(input)
    }

    override fun keyPressed(input: KeyInput): Boolean {
        if (input.key == /* ESCAPE */ 256) {
            MC.currentScreen?.close()
        }
        if (searchBox.isFocused) {
            if (input.key == /* E */ 69) {
                return true
            }
            return searchBox.keyPressed(input).also {
                onSearchChanged(searchBox.text)
            }
        }
        return super.keyPressed(input)
    }

    private fun buildHeader(): FlowLayout {
        val openFolderButton = Components.button(Text.of("\uD83D\uDDC0")) {
            val dir = FileHandler.currentDir()
            Util.getOperatingSystem().open(dir)
        }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.openfolder.description")))
        }

        return Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
            gap(2)
            children(
                listOf(
                    searchBox,
                    openFolderButton
                )
            )
        }
    }

    fun explorerEntry(name: String, index: Int): FlowLayout {
        val file: File? = filteredFiles.getOrNull(index)?.let { FileHandler.getFile(it) }

        val entry = when {
            (file?.isDirectory == true) ->
                FolderEntryComponent.create(Sizing.fill(), Sizing.fixed(25), index, file)

            (itemExtensions.any { file?.name?.lowercase()?.endsWith(it) == true }) ->
                ItemEntryComponent.create(Sizing.fill(), Sizing.fixed(25), index, file!!)

            (htslExtensions.any { file?.name?.lowercase()?.endsWith(it) == true }) ->
                ScriptEntryComponent.create(Sizing.fill(), Sizing.fixed(25), index, file!!)

            else -> throw IllegalStateException("Unknown file type.")
        }

        return entry.apply {
            val icon = Components.texture(this.icon, 0, 0, 16, 16, 16, 16)
            val label = Components.label(
                name.lastIndexOf('.').let { dot ->
                    if (dot > 0) {
                        val base = name.substring(0, dot)
                        val extension = name.substring(dot)
                        Text.literal(base).append(Text.literal(extension).withColor(0x808080))
                    } else Text.literal(name)
                }
            )

            surface(Surface.DARK_PANEL)
            gap(4)
            padding(Insets.of(5))
            alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER)

            children(
                listOf(
                    icon,
                    label
                )
            )
        }
    }

    val content: FlowLayout = Containers.verticalFlow(Sizing.fill(), Sizing.content()).apply {
        gap(1)
        margins(Insets.right(6))

        children(filteredFiles.mapIndexed { index, fileName ->
            explorerEntry(fileName, index)
        })
    }

    fun updateButtons() {
        val buttons = this.uiAdapter.rootComponent
            .childById(FlowLayout::class.java, "base")
            .childById(FlowLayout::class.java, "buttons")
        buttons.queue {
            buttons.clearChildren()
            if (focus == null) {
                buttons.verticalSizing(Sizing.fixed(0))
                return@queue
            }
            buttons.verticalSizing(Sizing.fixed(20))
            buttons.children(focus!!.buildContextButtons())
        }
    }

    fun refreshExplorer(queue: Boolean = false) {
        if (queue) {
            content.queue { refreshExplorer(false) }
            return
        }
        content.clearChildren()
        content.children(
            filteredFiles.mapIndexed { index, fileName ->
                explorerEntry(fileName, index)
            }
        )
    }

    private fun buildExplorer(): ScrollContainer<FlowLayout> {
        refreshExplorer()
        return Containers.verticalScroll(Sizing.expand(), Sizing.expand(), content).apply {
            surface(Surface.VANILLA_TRANSLUCENT)
            margins(Insets.vertical(5))
            padding(Insets.of(2))
            scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            scrollbarThiccness(4)
        }
    }

    val dropdown: DropdownComponent = buildDropdown()

    private fun buildContextButtons(): FlowLayout {
        return Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(0)).apply {
            id("buttons")
            gap(2)
        }
    }

    fun breadcrumb(name: String, index: Int): LabelComponent {
        return Components.label(Text.literal(name)).apply {
            mouseEnter().subscribe {
                text(Text.literal(name).withColor(0x808080))
                cursorStyle(CursorStyle.HAND)
            }
            mouseLeave().subscribe {
                text(Text.literal(name))
                cursorStyle(CursorStyle.POINTER)
            }
            mouseDown().subscribe { _, _ ->
                FileExplorerHandler.onBreadcrumbClicked(name, index)
                false
            }
        }
    }

    val breadcrumbs: FlowLayout = buildBreadcrumbs()

    fun refreshBreadcrumbs() {
        val subDir = FileHandler.subDir
        breadcrumbs.queue {
            breadcrumbs.clearChildren()
            breadcrumbs.child(breadcrumb("imports", -1))
            if (subDir.isEmpty()) return@queue
            val split = subDir.split("/")
            for (i in split.indices) {
                val name = split[i]
                breadcrumbs.child(Components.label(Text.literal(">").withColor(0x505050)))
                breadcrumbs.child(breadcrumb(name, i))
            }
        }
    }

    private fun buildBreadcrumbs(): FlowLayout {
        return Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
            gap(4)
            alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
            children(
                listOf(
                    breadcrumb("imports", -1),
                )
            )
        }
    }

    fun buildDropdown(): DropdownComponent {
        return DropdownComponent.create(Sizing.fixed(50), Sizing.content()).apply {
            surface(Surface.DARK_PANEL)
            padding(Insets.of(2))
            positioning(Positioning.absolute(0, 0))
            children(
                listOf(
                    Components.button(
                        Text.translatable("htslreborn.explorer.button.script.import.add")
                    ) { }.apply {
                        id("add")
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.import.add.description")))
                    },
                    Components.button(
                        Text.translatable("htslreborn.explorer.button.script.import.replace")
                    ) { }.apply {
                        id("replace")
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.import.replace.description")))
                    },
                    Components.button(
                        Text.translatable("htslreborn.explorer.button.script.import.update")
                    ) { }.apply {
                        id("update")
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.explorer.button.script.import.update.description")))
                    },
                )
            )
        }
    }

    fun buildImportScreen(fileName: String): FlowLayout {
        val label = Components.label(Text.literal(fileName))
        val cancelButton = Components.button(Text.translatable("htslreborn.importing.button.cancel")) {
            SystemsAPI.getHousingImporter().cancelImport()
            hideImportScreen()
        }
        val timeRemaining = TimeRemainingComponent()
        return Containers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
            id("importScreen")
            surface(Surface.flat(0xcc000000.toInt()).and(Surface.blur(6f, 6f)))
            positioning(Positioning.absolute(0, 0))
            horizontalAlignment(HorizontalAlignment.CENTER)
            verticalAlignment(VerticalAlignment.CENTER)
            gap(5)

            children(
                listOf(
                    label,
                    timeRemaining,
                    cancelButton
                )
            )
        }
    }

    fun showImportScreen(fileName: String) {
        val importScreen = buildImportScreen(fileName)
        this.uiAdapter.rootComponent.childById(FlowLayout::class.java, "base").child(importScreen)
    }

    fun hideImportScreen() {
        val base = this.uiAdapter.rootComponent.childById(FlowLayout::class.java, "base")
        val importScreen = base.childById(FlowLayout::class.java, "importScreen")
        if (importScreen != null) {
            base.removeChild(importScreen)
        }
    }

    public override fun build(root: FlowLayout) {
        val accessor =
            (MC.currentScreen as? HandledScreenAccessor) ?: throw IllegalStateException("Could not get accessor")

        refreshFiles(true)

        root.apply {
            sizing(Sizing.fixed(accessor.getGuiLeft()), Sizing.expand())
            padding(Insets.of(5))

            child(
                Containers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
                    id("base")
                    surface(Surface.DARK_PANEL)
                    padding(Insets.of(5))

                    children(
                        listOf(
                            buildTitle(),
                            buildHeader(),
                            buildExplorer(),
                            buildContextButtons(),
                            breadcrumbs,

                            // Below is things that go on top of the screen generally an absolute positioning
                            dropdown
                        )
                    )

                    if (importing) {
                        child(buildImportScreen(importingFile))
                    }
                }
            )
        }
    }
}