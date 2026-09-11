package llc.redstone.htslreborn.hook

import dynamic_fps.impl.DynamicFPSMod

class DynamicFPSHook {
    fun disable() {
        if (!DynamicFPSMod.isDisabled()) {
            DynamicFPSMod.toggleDisabled()
        }
    }

    fun enable() {
        if (DynamicFPSMod.isDisabled()) {
            DynamicFPSMod.toggleDisabled()
        }
    }
}
