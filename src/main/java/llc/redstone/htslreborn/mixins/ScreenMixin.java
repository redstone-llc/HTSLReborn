package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.accessors.ScreenAccessor;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Screen.class)
public class ScreenMixin implements ScreenAccessor {
    @Override
    public boolean isScreenInitialized() {
        return this.screenInitialized;
    }

    @Override
    public void setScreenInitialized(boolean initialized) {
        this.screenInitialized = initialized;
    }

    @Shadow
    private boolean screenInitialized = false;


}
