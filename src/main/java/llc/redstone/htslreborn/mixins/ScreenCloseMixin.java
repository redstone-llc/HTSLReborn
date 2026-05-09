package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.HTSLReborn;
import llc.redstone.systemsapi.util.MenuUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientPlayNetworkHandler.class, priority = 1001)
public class ScreenCloseMixin {
    @Inject(method = "onCloseScreen", at = @At("RETURN"))
    public void htslreborn$completeCloseAfterScreenClears(CallbackInfo ci) {
        if (!HTSLReborn.INSTANCE.getImporting() && !HTSLReborn.INSTANCE.getExporting()) return;
        MenuUtils.INSTANCE.completeOnClose$systemsapi();
    }
}
