package llc.redstone.htslreborn.overlay

import llc.redstone.htslreborn.HTSLReborn.MC
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

        context.drawString(font, "HTSL Reborn Debug HUD", 10, 10, 0xFFFFFFFF.toInt())
        context.drawString(font, "Queue: ${Queue.size()}", 10, 25, 0xFFFFFFFF.toInt())
        context.drawString(font, "Current Operation: ${Queue.current}", 10, 40, 0xFFFFFFFF.toInt())
    }
}