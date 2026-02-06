package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.HTSLReborn;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(SoundSystem.class)
public class SoundSilencerMixin {

    @Inject(
            method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cancelSound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir) {
        if (!HTSLReborn.INSTANCE.getCONFIG().getSilenceImportSounds()) return;
        if (!HTSLReborn.INSTANCE.getImporting() && !HTSLReborn.INSTANCE.getExporting()) return;

        Identifier soundId = sound.getId();
        if (SoundEvents.BLOCK_NOTE_BLOCK_PLING.matchesId(soundId)) {
            cir.cancel();
        }
    }

}
