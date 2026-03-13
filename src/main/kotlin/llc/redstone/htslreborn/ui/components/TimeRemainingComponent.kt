package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.core.OwoUIGraphics
import llc.redstone.systemsapi.SystemsAPI
import net.minecraft.text.Text
import kotlin.math.roundToInt

class TimeRemainingComponent : LabelComponent(
    Text.of("")
) {
    override fun draw(graphics: OwoUIGraphics, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        if (SystemsAPI.getHousingImporter().getTimeRemaining() != null)
            text(Text.translatable("htslreborn.importing.timeremaining", SystemsAPI.getHousingImporter().getTimeRemaining()?.roundToInt()))

        super.draw(graphics, mouseX, mouseY, partialTicks, delta)
    }
}