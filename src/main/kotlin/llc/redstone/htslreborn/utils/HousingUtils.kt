package llc.redstone.htslreborn.utils

import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.util.StringUtil
import net.minecraft.world.scores.DisplaySlot


object HousingUtils {
    private val houseLine = Regex("(?i)You are in (.+), by .+")

    @Volatile
    private var currentHouse: String? = null

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

    /** House name from the latest tab-list footer packet. */
    fun houseName(): String? = currentHouse

    fun onTabFooter(footer: Component?) {
        val plain = footer?.string?.let(StringUtil::stripColor).orEmpty()
        currentHouse = houseLine.find(plain)?.groupValues?.get(1)?.trim()?.ifBlank { null }
    }

    fun clearHouse() {
        currentHouse = null
    }
}
