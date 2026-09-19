package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.ui.HTSLScreen;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void htslreborn$charTyped(long window, CharacterEvent event, CallbackInfo ci) {
        if (!HTSLScreen.shouldBeVisible()) return;
        if (HTSLScreen.getINSTANCE().charTyped(event)) {
            ci.cancel();
        }
    }
}
