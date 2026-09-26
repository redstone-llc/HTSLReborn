package llc.redstone.htslreborn.overlay

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.queue.Progress
import llc.redstone.htslreborn.queue.Queue
import llc.redstone.htslreborn.utils.TextUtils
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.resources.Identifier

object DebugHud {
    private val ID = Identifier.fromNamespaceAndPath("htslreborn", "debug_hud")

    fun register() {
        HudElementRegistry.addLast(ID) { context, _ ->
            render(context)
        }
    }

    fun render(context: GuiGraphicsExtractor) {
        if (!Queue.isActive) return
        val font = MC.font
        var y = 10
        fun line(text: String) {
            context.text(font, text, 10, y, 0xFFFFFFFF.toInt())
            y += 15
        }

        line(TextUtils.translate("htslreborn.debug.title"))
        line(TextUtils.translate("htslreborn.debug.queue", Queue.size()))
        line(TextUtils.translate("htslreborn.debug.operation", "${Queue.current}"))
        line(TextUtils.translate("htslreborn.debug.gui_context", "${Queue.guiContext}"))
        line(TextUtils.translate("htslreborn.debug.attempts", Queue.attempts))
        if (Queue.paused) line(TextUtils.translate("htslreborn.debug.paused"))
        line(TextUtils.translate("htslreborn.debug.session", Queue.session?.describe() ?: TextUtils.translate("htslreborn.debug.none")))

        if (Progress.active) {
            val p = Progress
            line(TextUtils.translate("htslreborn.debug.progress", p.completedOps, p.totalOps, (p.fraction * 100).toInt()))
            line(TextUtils.translate("htslreborn.debug.elapsed", p.format(p.elapsedMs)))
            line(TextUtils.translate("htslreborn.debug.total", p.format(p.displayTotalMs, p.indeterminate), p.format(p.rawTotalMs)))
        }

        for ((key, avg) in Progress.averages.entries.sortedBy { it.key }) {
            line("  $key: ${avg.toInt()}ms")
        }
    }
}