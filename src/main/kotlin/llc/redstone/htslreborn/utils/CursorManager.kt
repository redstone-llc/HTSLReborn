package llc.redstone.htslreborn.utils

import com.mojang.blaze3d.platform.cursor.CursorTypes
import net.minecraft.client.Minecraft

object CursorManager {
    private var handRequested = false
    private var iBeamRequested = false

    fun setHandCursor() {
        handRequested = true
    }

    fun setIBeamCursor() {
        iBeamRequested = true
    }

    fun resetCursor() {
        //? if <26.3 {
        val window = Minecraft.getInstance().window
        when {
            iBeamRequested -> CursorTypes.IBEAM.select(window)
            handRequested -> CursorTypes.POINTING_HAND.select(window)
            else -> CursorTypes.ARROW.select(window)
        }
        //? } else {
        // when {
        //     iBeamRequested -> CursorTypes.IBEAM.select()
        //     handRequested -> CursorTypes.POINTING_HAND.select()
        //     else -> CursorTypes.ARROW.select()
        // }
        //? }
        handRequested = false
        iBeamRequested = false
    }
}