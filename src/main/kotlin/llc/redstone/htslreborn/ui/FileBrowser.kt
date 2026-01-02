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

    override fun build(root: FlowLayout) {
        val accessor = (MC.currentScreen as? HandledScreenAccessor) ?: return //TODO: throw error

        root.sizing(Sizing.fixed(accessor.getGuiLeft()), Sizing.expand())
        root.padding(Insets.of(5))

        val background = Containers.verticalFlow(Sizing.fill(), Sizing.fill())

        background
            .surface(Surface.DARK_PANEL)
            .padding(Insets.of(5))

        // Header
        val searchBox = Components.textBox(Sizing.expand())
        searchBox.verticalSizing(Sizing.fill())
        searchBox.setPlaceholder(Text.literal("Search"))

        val openFolderButton = Components.button(Text.of("Folder")) { button ->
            TODO("Not implemented yet")
        }
        openFolderButton.sizing(Sizing.content(), Sizing.fill())

        val header = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20))
            .child(searchBox)
            .child(openFolderButton)


        // File Explorer
        val fileDummies = mutableListOf<FlowLayout>()
        repeat(20) {
            fileDummies.add(
                Containers.verticalFlow(Sizing.fill(), Sizing.fixed(25))
                    .also {
                        it.surface(Surface.PANEL)
                    }
            )

        }

        val explorer = Containers.verticalScroll(
            Sizing.expand(), Sizing.expand(),
            Containers.verticalFlow(Sizing.fill(), Sizing.content())
                .children(fileDummies)
                .margins(Insets.right(4))
        )

        explorer
            .surface(Surface.PANEL_INSET)
        explorer
            .margins(Insets.vertical(5))
        explorer
            .padding(Insets.of(2))
        explorer
            .scrollbar(ScrollContainer.Scrollbar.vanillaFlat())
            .scrollbarThiccness(4)

        // Action buttons
        val actionButtons = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20))
        actionButtons.child(
            Components.button(Text.of("Import")) { button ->}
        )
        actionButtons.child(
            Components.button(Text.of("Export")) { button ->}
        )
        actionButtons.child(
            Containers.horizontalFlow(Sizing.expand(), Sizing.fill())
        )
        actionButtons.child(
            Components.button(Text.of("Open")) { button ->}
        )
        actionButtons.child(
            Components.button(Text.of("Delete")) { button ->}
        )


        //Breadcrumbs
        val breadcrumbs = Containers.horizontalFlow(Sizing.fill(), Sizing.fixed(20))
        breadcrumbs
            .alignment(HorizontalAlignment.CENTER, VerticalAlignment.CENTER)
        breadcrumbs
            .gap(4)

        fun breadcrumbDummy(name: String): LabelComponent {
            val dummy = Components.label(Text.literal(name))
            dummy.mouseEnter().subscribe {
                dummy.text(Text.literal(name).withColor(0x808080))
                dummy.cursorStyle(CursorStyle.HAND)
            }
            dummy.mouseLeave().subscribe {
                dummy.text(Text.literal(name))
                dummy.cursorStyle(CursorStyle.POINTER)
            }
            return dummy
        }

        breadcrumbs
            .children(listOf(
                breadcrumbDummy("imports"),
                Components.label(Text.literal(">").withColor(0x505050)),
                breadcrumbDummy("Project"),
            ))

        background.children(listOf(
            header,
            explorer,
            actionButtons,
            breadcrumbs,
        ))

        root.child(background)
    }
}