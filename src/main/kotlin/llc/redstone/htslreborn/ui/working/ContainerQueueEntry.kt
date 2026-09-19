package llc.redstone.htslreborn.ui.working

import llc.redstone.htslreborn.HTSLReborn
import llc.redstone.htslreborn.data.ScriptContainer
import llc.redstone.htslreborn.queue.BuildableContainer
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.queue.differ.Differ
import llc.redstone.htslreborn.queue.exporter.Exporter
import llc.redstone.htslreborn.queue.importer.Importer
import llc.redstone.htslreborn.ui.Icon
import llc.redstone.htslreborn.ui.IconWidget
import llc.redstone.htslreborn.utils.TextUtils.drawEllipsis
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import java.nio.file.Path

data class ContainerQueueEntry(
    val container: ScriptContainer?, val context: BuildableContainer, val source: Path? = null
) : IconWidget(
    0, 0, 202, 15, Component.literal(container?.target?.name ?: "Unknown Container")
) {
    companion object {
        val PENDING = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/container/pending.png")
        val ACTIVE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/container/active.png")
        val COMPLETE = Identifier.fromNamespaceAndPath("htslreborn", "textures/ui/container/complete.png")
    }

    fun getState(): State {
        val index = Queue.containers.indexOf(this)
        val activeIndex = if (Queue.session != null) Queue.tasksStarted - 1 else Queue.tasksStarted
        return when {
            index < activeIndex -> State.COMPLETE
            index == activeIndex -> State.ACTIVE
            else -> State.PENDING
        }
    }

    override val icons: List<Icon>
        get() = when (getState()) {
            State.PENDING -> listOf(
                Icon(187, 0) {
                    Queue.containers.remove(this)
                }
            )
            else -> listOf()
        }
    override val BACKGROUND: Identifier
        get() = when (getState()) {
            State.PENDING -> PENDING
            State.ACTIVE -> ACTIVE
            State.COMPLETE -> COMPLETE
        }

    override fun extractWidgetRenderState(guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) {
        super.extractWidgetRenderState(guiGraphics, mouseX, mouseY, delta)
        val contextName = when (context) {
            is Importer -> "Import"
            is Exporter -> "Export"
            is Differ -> "Diff"
            else -> "Unknown"
        }

        val clipRight = if (getState() == State.PENDING) 187 else 198
        guiGraphics.drawEllipsis(
            HTSLReborn.MC.font,
            Component.literal("$contextName ")
                .withColor(0x3F3F3F)
                .append(
                    Component.literal("${source?.fileName} > ${container?.target?.name ?: "Default"}")
                        .withColor(0x666666)
                ),
            x + 14,
            y + 4,
            clipRight - 14,
            0xFFFFFFFF.toInt(),
            false
        )
    }

    override fun updateWidgetNarration(narrationElementOutput: NarrationElementOutput) {
    }

    enum class State {
        PENDING, ACTIVE, COMPLETE
    }
}