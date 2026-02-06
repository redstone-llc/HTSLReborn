package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.HTSLReborn;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatSilencerMixin {

    @Unique
    private final List<Pattern> HIDDEN_MESSAGES = List.of(
            Pattern.compile("^Added action .+!$"),
            Pattern.compile("^\n\n.+:\nPlease use the chat to provide the value you wish to set\\.\n.*$")
    );

    @Inject(
            method = "onGameMessage",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if (!HTSLReborn.INSTANCE.getCONFIG().getSilenceImportMessages()) return;
        if (!HTSLReborn.INSTANCE.getImporting() && !HTSLReborn.INSTANCE.getExporting()) return;

        String message = packet.content().getString();
        if (message == null) return;

        if (HIDDEN_MESSAGES.stream().anyMatch(p -> p.matcher(message).matches())) {
            ci.cancel();
        }
    }

}
