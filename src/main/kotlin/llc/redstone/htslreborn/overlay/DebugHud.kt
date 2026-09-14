package llc.redstone.htslreborn.overlay

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.importer.ImportProgress
import llc.redstone.htslreborn.importer.ImportSession
import llc.redstone.htslreborn.importer.Queue
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.Identifier

object DebugHud {
    private val ID = Identifier.fromNamespaceAndPath("htslreborn", "debug_hud")

    fun register() {
        HudElementRegistry.addLast(ID) { context, _ ->
            render(context)
        }
    }

    fun render(context: GuiGraphics) {
        val font = MC.font
        var y = 10
        fun line(text: String) {
            context.drawString(font, text, 10, y, 0xFFFFFFFF.toInt())
            y += 15
        }

        line("HTSL Reborn Debug HUD")
        line("Queue: ${Queue.size()}")
        line("Current Operation: ${Queue.current}")
        line("Gui Context: ${Queue.guiContext}")
        line("Attempts: ${Queue.attempts}")
        if (Queue.paused) line("PAUSED")
        line("Checkpoint: ${ImportSession.checkpoint?.path} (base ${ImportSession.checkpointBase})")

        if (ImportProgress.active) {
            val p = ImportProgress
            line("Progress: ${p.completedOps}/${p.totalOps} (${(p.fraction * 100).toInt()}%)")
            line("Elapsed: ${p.format(p.elapsedMs)}")
            line("ETA: ${p.format(p.displayRemainingMs, p.indeterminate)}")
        }

        for ((key, avg) in ImportProgress.averages.entries.sortedBy { it.key }) {
            line("  $key: ${avg.toInt()}ms")
        }
    }
}