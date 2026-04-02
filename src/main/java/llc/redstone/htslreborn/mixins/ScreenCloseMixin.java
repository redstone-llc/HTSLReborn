package llc.redstone.htslreborn.mixins;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 1001)
public class ScreenCloseMixin {
    @Inject(method = "onCloseScreen", at = @At("HEAD"), cancellable = true)
    public void htslreborn$onCloseScreen(CallbackInfo ci) {
//        if (HTSLReborn.INSTANCE.getExporting() || HTSLReborn.INSTANCE.getImporting()) {
//            ci.cancel();
//        }
    }
}
