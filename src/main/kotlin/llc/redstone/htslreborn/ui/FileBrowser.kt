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
import llc.redstone.htslreborn.accessors.HandledScreenAccessor
import llc.redstone.htslreborn.accessors.ScreenAccessor
import llc.redstone.htslreborn.ui.FileBrowserHandler.onSearchChanged
import llc.redstone.htslreborn.ui.FileHandler.filteredFiles
import llc.redstone.htslreborn.ui.FileHandler.refreshFiles
import llc.redstone.htslreborn.ui.components.DropdownComponent
import llc.redstone.htslreborn.ui.components.FocusableFlowLayout
import net.minecraft.client.gui.tooltip.Tooltip
import net.minecraft.client.input.CharInput
import net.minecraft.client.input.KeyInput
import net.minecraft.text.Text

class FileBrowser() : BaseOwoScreen<FlowLayout>() {
    companion object {
        var INSTANCE = FileBrowser()
    }

    override fun createAdapter(): OwoUIAdapter<FlowLayout?> {
        return OwoUIAdapter.create(this, Containers::verticalFlow)
    }

    private fun FlowLayout.hoverCursor(
        hover: CursorStyle = CursorStyle.HAND,
        normal: CursorStyle = CursorStyle.POINTER
    ) = apply {
        mouseEnter().subscribe { cursorStyle(hover) }
        mouseLeave().subscribe { cursorStyle(normal) }
    }

    private fun buildTitle(): Component {
        return Components.label(Text.translatable("htslreborn.menu.title")).apply {
            sizing(Sizing.fill(), Sizing.content())
            horizontalTextAlignment(HorizontalAlignment.CENTER)
            verticalTextAlignment(VerticalAlignment.CENTER)
            margins(Insets.of(2))
        }
    }

    private val searchBox = Components.textBox(Sizing.expand()).apply {
        verticalSizing(Sizing.fill())
        setPlaceholder(Text.translatable("htslreborn.menu.searchbox"))
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
        val openFolderButton = Components.button(Text.of("\uD83D\uDDC0")) { /*...*/ }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.openfolder.description")))
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
        val icon = Components.label(Text.of("[ICON]")) // TODO: replace with Components.sprite()
        val label = Components.label(Text.of(name))

        return FocusableFlowLayout.create(Sizing.fill(), Sizing.fixed(25), index).apply {
            surface(Surface.DARK_PANEL)
            gap(4)
            padding(Insets.of(5).withLeft(8).withRight(8))
            alignment(HorizontalAlignment.LEFT, VerticalAlignment.CENTER)
            hoverCursor()

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

    fun refreshExplorer() {
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

    var fileButtons = mutableListOf<ButtonComponent>()

    private var importButton = Components.button(Text.translatable("htslreborn.menu.button.import")) { /*...*/ }.apply {
        setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.import.add.description")))
    }

    private val dropdown: DropdownComponent = buildDropdown()

    private fun buildButtons(): FlowLayout {
        val dropdownButton = Components.button(Text.of("↓")) { dropdown.handleDropdownButton(it) }
        val import = Containers.horizontalFlow(Sizing.content(), Sizing.fill()).apply {
            children(
                listOf(
                    importButton,
                    dropdownButton
                )
            )
        }

        val export = Components.button(Text.translatable("htslreborn.menu.button.export")) { /*...*/ }.apply {
            setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.export.description")))
        }

        val spacer = Components.spacer()

        val edit = Components.button(Text.of("✎")) { /*...*/ }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.openfile.description")))
        }

        val delete = Components.button(Text.of("\uD83D\uDDD1")) { /*...*/ }.apply {
            sizing(Sizing.fixed(20), Sizing.fill())
            setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.deletefile.description")))
        }

        fileButtons.clear()
        fileButtons.addAll(
            listOf(
                importButton,
                export,
                edit,
                delete
            )
        )

        return Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
            gap(2)
            allowOverflow(true)
            children(
                listOf(
                    import,
                    export,
                    spacer,
                    edit,
                    delete
                )
            )
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
                FileBrowserHandler.onBreadcrumbClicked(name, index)
                false
            }
        }
    }

    val breadcrumbs: FlowLayout = buildBreadcrumbs()

    fun refreshBreadcrumbs() {
        val subDir = FileHandler.subDir
        breadcrumbs.clearChildren()
        breadcrumbs.child(breadcrumb("imports", -1))
        if (subDir.isEmpty()) return
        val split = subDir.split("/")
        for (i in split.indices) {
            val name = split[i]
            breadcrumbs.child(Components.label(Text.literal(">").withColor(0x505050)))
            breadcrumbs.child(breadcrumb(name, i))
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
                    Components.button(Text.translatable("htslreborn.menu.button.import.add")) {}.apply {
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.import.add.description")))
                    },
                    Components.button(Text.translatable("htslreborn.menu.button.import.replace")) {}.apply {
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.import.replace.description")))
                    },
                    Components.button(Text.translatable("htslreborn.menu.button.import.update")) {}.apply {
                        horizontalSizing(Sizing.fill())
                        renderer(ButtonComponent.Renderer.flat(0x00000000, 0x50000000, 0x00000000))
                        setTooltip(Tooltip.of(Text.translatable("htslreborn.menu.button.import.update.description")))
                    },
                )
            )
            actionComponent(importButton)
        }
    }

    public override fun build(root: FlowLayout) {
        val accessor = (MC.currentScreen as? HandledScreenAccessor) ?: return //TODO: throw error

        refreshFiles()

        root.apply {
            sizing(Sizing.fixed(accessor.getGuiLeft()), Sizing.expand())
            padding(Insets.of(5))

            child(
                Containers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
                    id("container")
                    surface(Surface.DARK_PANEL)
                    padding(Insets.of(5))

                    children(
                        listOf(
                            buildTitle(),
                            buildHeader(),
                            buildExplorer(),
                            buildButtons(),
                            breadcrumbs
                        )
                    )
                }
            )

            child(dropdown)
        }

//        // Importing screen
//        root.apply {
//            val title = Components.label(Text.of("Importing 'test_file.htsl'"))
//            val countdown = Components.label(Text.of("Time Remaining: 34s"))
//            val cancelButton = Components.button(Text.of("Cancel")) { /*...*/ }
//
//            val importingScreen = Containers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
//                surface(Surface.flat(0xcc000000.toInt()).and(Surface.blur(6f, 6f)))
//                positioning(Positioning.absolute(0, 0))
//                horizontalAlignment(HorizontalAlignment.CENTER)
//                verticalAlignment(VerticalAlignment.CENTER)
//                gap(5)
//
//                children(listOf(
//                    title,
//                    countdown,
//                    cancelButton,
//                ))
//            }
//
//            child(importingScreen)
//        }
    }
}