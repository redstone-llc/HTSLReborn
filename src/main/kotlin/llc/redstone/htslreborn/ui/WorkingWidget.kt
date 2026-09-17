package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.differ.DiffSession
import llc.redstone.htslreborn.queue.exporter.ExportSession
import llc.redstone.htslreborn.queue.importer.ImportSession
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class WorkingWidget(x: Int, y: Int) :
    AbstractWidget(x, y, 176, 222, Component.literal("Working")) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/working/working.png")
    }

    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        val session = Queue.session ?: return

        this.renderBackground(guiGraphics, mouseX, mouseY, delta)

        guiGraphics.drawString(
            MC.font,
            Component.literal(
                when (session) {
                    is ImportSession -> "Importing"
                    is ExportSession -> "Exporting"
                    is DiffSession -> if (session.phase == DiffSession.Phase.EXPORT) "Exporting" else "Importing"
                    else -> "Unknown Status"
                }
            ),
            x + 7,
            y + 8,
            0xFF3F3F3F.toInt(),
            false
        )

        if (session is ImportSession || session is DiffSession) {
            val file = when (session) {
                is ImportSession -> session.source
                is DiffSession -> session.source
                else -> null
            }

            val container = when (session) {
                is ImportSession -> session.containers?.getOrNull(session.checkpoint?.container ?: -1)
                is DiffSession -> session.containers?.getOrNull(session.container)
                else -> null
            }



            guiGraphics.drawString(
                MC.font,
                Component.literal("from ")
                    .withColor(0x666666)
                    .append(
                        Component.literal("'${file?.fileName ?: "Unknown"}'")
                            .withColor(0x3F3F3F)
                    ),
                x + 7,
                y + 20,
                0xFFFFFFFF.toInt(),
                false
            )

            guiGraphics.drawString(
                MC.font,
                Component.literal("into ")
                    .withColor(0x666666)
                    .append(
                        Component.literal("'${container?.target?.name ?: "Unknown"}${container?.target?.trigger ?: ""}' ")
                            .withColor(0x3F3F3F)
                    )
                    .append(
                        Component.literal(
                            "(" + (container?.context?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                                ?: "Unknown") + ")")
                            .withColor(0x666666)
                    ),
                x + 7,
                y + 32,
                0xFFFFFFFF.toInt(),
                false
            )
        }
    }

    private fun renderBackground(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        guiGraphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            x,
            y,
            0.0f,
            0.0f,
            width,
            height,
            width,
            height
        )
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {

    }
}