package llc.redstone.htslreborn.utils

import net.minecraft.client.Minecraft
import net.minecraft.util.StringUtil
import net.minecraft.world.scores.DisplaySlot


object HousingUtils {
    fun isInHousing(): Boolean {
        val scoreboard = Minecraft.getInstance().level?.scoreboard

        if (scoreboard != null) {
            val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR)
            if (objective != null) {
                val name: String = StringUtil.stripColor(objective.name)
                return name == "housing"
            }
        }
        return false
    }
}