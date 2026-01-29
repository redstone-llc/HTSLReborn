package llc.redstone.htslreborn.ui.components

import io.wispforest.owo.ui.component.LabelComponent
import io.wispforest.owo.ui.core.OwoUIDrawContext
import llc.redstone.systemsapi.SystemsAPI
import net.minecraft.text.Text

class TimeRemainingComponent() : LabelComponent(
    Text.of("")
) {
    override fun draw(context: OwoUIDrawContext?, mouseX: Int, mouseY: Int, partialTicks: Float, delta: Float) {
        text(Text.translatable("htslreborn.importing.timeremaining", SystemsAPI.getHousingImporter().getTimeRemaining()?.toInt()))

        super.draw(context, mouseX, mouseY, partialTicks, delta)
    }
}