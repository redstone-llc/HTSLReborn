package llc.redstone.htslreborn.ui

import llc.redstone.htslreborn.HTSLReborn.MC
import llc.redstone.htslreborn.accessor.HandledScreenAccessor
import llc.redstone.htslreborn.queue.Queue
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.network.chat.Component

class HTSLScreen: Screen(Component.literal("Working Screen")) {
    companion object {
        @JvmStatic var INSTANCE = HTSLScreen()

        val housingTitles = listOf(
            "Housing Menu",
            "Systems",
            "Actions",
            "Functions",
            "Regions",
            "Commands",
            "Custom Menus",
            "Edit Menu",
            "NPCs",
            "Edit NPC"
        )

        @JvmStatic
        fun shouldBeVisible(): Boolean {
            return Queue.isActive || housingTitles.any { title -> MC.screen?.title?.string?.contains(title) == true } || MC.screen is InventoryScreen/* || MC.screen is CreativeModeInventoryScreen*/
        }
    }

    val imageWidth = 176

    override fun init() {
        super.init()

        val accessor = MC.screen as? HandledScreenAccessor ?: return
        val menuSize = getMenuHeight()
        val menuTop = if (Queue.isActive) (this.height - menuSize) / 2 else accessor.getGuiTop()

        if (Queue.isActive) {
            addRenderableWidget(WorkingTopbarWidget((this.width - imageWidth) / 2, menuTop - 19))
            addRenderableWidget(BottombarWidget((this.width - imageWidth) / 2, (this.height - menuSize) / 2 + menuSize))
            addRenderableWidget(WorkingWidget((this.width - imageWidth) / 2, (this.height - 222) / 2))
            return
        }

        addRenderableWidget(TopbarWidget((this.width - imageWidth) / 2, menuTop - 19))
    }

    fun getMenuHeight(): Int {
        if (Queue.isActive) return 222
        return 144
    }
}