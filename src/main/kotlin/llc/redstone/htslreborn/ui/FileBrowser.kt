package llc.redstone.htslreborn.ui

import io.wispforest.owo.ui.base.BaseOwoScreen
import io.wispforest.owo.ui.component.Components
import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.container.Containers
import io.wispforest.owo.ui.container.FlowLayout
import io.wispforest.owo.ui.container.ScrollContainer
import io.wispforest.owo.ui.core.*
import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.accessors.HandledScreenAccessor
import net.minecraft.text.Text

class FileBrowser : BaseOwoScreen<FlowLayout>() {
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

    private fun buildHeader(): FlowLayout {
        val searchBox = Components.textBox(Sizing.expand()).apply {
            verticalSizing(Sizing.fill())
            setPlaceholder(Text.literal("Search"))
        }

        val openFolderButton = Components.button(Text.of("\uD83D\uDDC0")) { /*...*/ }
            .apply { sizing(Sizing.content(), Sizing.fill()) }

        return Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
            child(searchBox)
            child(openFolderButton)
            gap(2)
        }
    }

    private fun buildExplorer(): ScrollContainer<FlowLayout> {
        val fileRows: List<FlowLayout> = List(20) {
            Containers.verticalFlow(Sizing.fill(), Sizing.fixed(25)).apply {
                surface(Surface.PANEL)
                hoverCursor()
            }
        }

        val content = Containers.verticalFlow(Sizing.fill(), Sizing.content()).apply {
            children(fileRows)
            gap(1)
            margins(Insets.right(4))
        }

        return Containers.verticalScroll(Sizing.expand(), Sizing.expand(), content).apply {
            surface(Surface.PANEL_INSET)
            margins(Insets.vertical(5))
            padding(Insets.of(2))
            scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            scrollbarThiccness(4)
        }
    }

    private fun buildButtons(): FlowLayout {
        return Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
            gap(2)
            allowOverflow(true)

            child(
                Containers.horizontalFlow(Sizing.content(), Sizing.fill()).apply {
                    children(
                        listOf(
                            Components.button(Text.of("Import")) { /*...*/ },
                            Components.button(Text.of("↓")) { /*...*/ }
                        )
                    )
                    allowOverflow(true)
                }
            )

            child(Components.button(Text.of("Export")) { /*...*/ })
            child(Containers.horizontalFlow(Sizing.expand(), Sizing.fill())) // spacer
            child(Components.button(Text.of("✎")) { /*...*/ })
            child(Components.button(Text.of("\uD83D\uDDD1")) { /*...*/ })
        }
    }

    private fun buildBreadcrumbs(): FlowLayout {
        fun crumb(name: String): LabelComponent =
            Components.label(Text.literal(name)).apply {
                mouseEnter().subscribe {
                    text(Text.literal(name).withColor(0x808080))
                    cursorStyle(CursorStyle.HAND)
                }
                mouseLeave().subscribe {
                    text(Text.literal(name))
                    cursorStyle(CursorStyle.POINTER)
                }
            }

        return Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20)).apply {
            alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
            gap(4)
            children(
                listOf(
                    crumb("imports"),
                    Components.label(Text.literal(">").withColor(0x505050)),
                    crumb("Project"),
                )
            )
        }
    }

    override fun build(root: FlowLayout) {
        val accessor = (MC.currentScreen as? HandledScreenAccessor) ?: return //TODO: throw error

        root.apply {
            sizing(Sizing.fixed(accessor.getGuiLeft()), Sizing.expand())
            padding(Insets.of(5))

            child(
                Containers.verticalFlow(Sizing.fill(), Sizing.fill()).apply {
                    surface(Surface.DARK_PANEL)
                    padding(Insets.of(5))

                    children(listOf(
                        buildHeader(),
                        buildExplorer(),
                        buildButtons(),
                        buildBreadcrumbs()
                    ))
                }
            )
        }
    }
}