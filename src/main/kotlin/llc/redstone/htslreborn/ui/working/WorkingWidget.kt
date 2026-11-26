package llc.redstone.htslreborn.ui.working

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.data.ImportContext
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.differ.DiffSession
import llc.redstone.htslreborn.queue.exporter.ExportSession
import llc.redstone.htslreborn.queue.importer.ImportSession
import llc.redstone.htslreborn.ui.HTSLScrollWidget
import llc.redstone.htslreborn.utils.TextUtils
import llc.redstone.htslreborn.utils.TextUtils.drawEllipsis
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier

class WorkingWidget(x: Int, y: Int) :
    AbstractWidget(x, y, 223, 222, Component.translatable("htslreborn.working.title")) {
    companion object {
        val BACKGROUND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/working/working.png")
        var scrollHeight = 0.0

        val COMMAND = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/command.png")
        val EVENT = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/event.png")
        val FUNCTION = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/function.png")
        val NPC = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/npc.png")
        val REGION = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/region.png")
        val CUSTOMMENU = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/icon/custommenu.png")
    }

    private val queueScroll = HTSLScrollWidget(0, 0, 206, 161, QueueLayout(0, 0, 206, 161), scrollHeight)

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        val session = Queue.session ?: return

        this.renderBackground(guiGraphics, mouseX, mouseY, delta)

        guiGraphics.text(
            MC.font,
            Component.translatable(
                when (session) {
                    is ImportSession -> "htslreborn.working.importing"
                    is ExportSession -> "htslreborn.working.exporting"
                    is DiffSession -> if (session.phase == DiffSession.Phase.EXPORT) "htslreborn.working.exporting" else "htslreborn.working.editing"
                    else -> "htslreborn.working.unknown_status"
                }
            ),
            x + 8,
            y + 8,
            0xFF3F3F3F.toInt(),
            false
        )

        guiGraphics.text(
            MC.font,
            Component.translatable(
                "htslreborn.working.task",
                Queue.tasksStarted,
                Queue.containers.size
            ),
            x + 170,
            y + 8,
            0xFF3F3F3F.toInt(),
            false
        )

        val input = when (session) {
            is ImportSession -> session.source?.fileName
            is DiffSession -> session.source?.fileName
            is ExportSession -> session.container?.let { containerText(it) }

            else -> null
        }

        val output = when (session) {
            is ImportSession -> session.container?.let { containerText(it) }

            is DiffSession -> session.container?.let { containerText(it) }

            is ExportSession -> session.source?.fileName?.toString()
            else -> null
        }

        val badgeTexture = when(session.container?.context) {
            ImportContext.COMMAND -> COMMAND
            ImportContext.EVENT -> EVENT
            ImportContext.FUNCTION -> FUNCTION
            ImportContext.NPC -> NPC
            ImportContext.REGION -> REGION
            ImportContext.CUSTOMMENU -> CUSTOMMENU
            else -> null
        }

        val badgeX = if (session is ExportSession) x + 8 else x + 121
        val badgeInput = session !is ExportSession
        guiGraphics.drawEllipsis(
            MC.font,
            input?.let { Component.literal(it.toString()) } ?: Component.translatable("htslreborn.working.unknown"),
            if (badgeInput || badgeTexture == null) x + 11 else x + 20,
            y + 22,
            if (badgeInput || badgeTexture == null) 92 else 83,
            0xFF3F3F3F.toInt(),
            false
        )

        guiGraphics.drawEllipsis(
            MC.font,
            output?.let { Component.literal(it) } ?: Component.translatable("htslreborn.working.unknown"),
            if (!badgeInput || badgeTexture == null) x + 121 else x + 130,
            y + 22,
            if (!badgeInput || badgeTexture == null) 92 else 83,
            0xFF3F3F3F.toInt(),
            false
        )

        if (badgeTexture != null) {
            guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                badgeTexture,
                badgeX,
                y + 22,
                0.0f,
                0.0f,
                7,
                7,
                7,
                7
            )
        }

        renderQueue(guiGraphics, mouseX, mouseY, delta)
    }

    private fun containerText(container: ScriptContainer): String {
        val name = container.target.name ?: TextUtils.translate("htslreborn.browser.default")
        return "$name ${container.target.trigger ?: ""}"
    }

    private fun renderQueue(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        queueScroll.setPosition(x + 9, y + 53)
        queueScroll.extractRenderState(guiGraphics, mouseX, mouseY, delta)
        scrollHeight = queueScroll.scrollAmount()
    }

    private fun renderBackground(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
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

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double
    ): Boolean {
        if (queueScroll.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(event: MouseButtonEvent, doubled: Boolean): Boolean {
        if (queueScroll.mouseClicked(event, doubled)) {
            return true
        }
        return super.mouseClicked(event, doubled)
    }

    override fun mouseDragged(event: MouseButtonEvent, offsetX: Double, offsetY: Double): Boolean {
        if (queueScroll.mouseDragged(event, offsetX, offsetY)) {
            return true
        }
        return super.mouseDragged(event, offsetX, offsetY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (queueScroll.mouseReleased(event)) {
            return true
        }
        return super.mouseReleased(event)
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
    }
}
