package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.utils.HousingUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class TabListMixin {
    @Inject(method = "handleTabListCustomisation", at = @At("HEAD"))
    private void htslreborn$onTabList(ClientboundTabListPacket packet, CallbackInfo ci) {
        // HEAD runs before ensureRunningOnSameThread, so this fires on the netty thread and again on the main thread.
        if (!Minecraft.getInstance().isSameThread()) return;
        HousingUtils.INSTANCE.onTabFooter(packet.footer());
    }
}
