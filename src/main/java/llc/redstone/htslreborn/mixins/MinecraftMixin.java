package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.utils.MenuUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            // The menu was closed, player is back in the game
            return;
        }

        MenuUtils.INSTANCE.onScreenOpen(screen);
    }
}