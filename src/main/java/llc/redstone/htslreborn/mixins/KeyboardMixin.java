package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.ui.FileExplorer;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.CharInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyboardMixin {
    @Inject(method = "onChar", at = @At("HEAD"))
    public void htslreborn$charTyped(long window, CharInput input, CallbackInfo ci) {
        if (!FileExplorer.inActionGui()) return;
        FileExplorer.Companion.getINSTANCE().charTyped(input);
    }
}
