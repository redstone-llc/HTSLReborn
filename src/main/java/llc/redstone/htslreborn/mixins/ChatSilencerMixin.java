package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.importer.Queue;
import llc.redstone.htslreborn.utils.InputUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.regex.Pattern;

@Mixin(value = ClientPacketListener.class)
public class ChatSilencerMixin {

    @Unique
    private static final List<Pattern> HIDDEN_MESSAGES = List.of(
            Pattern.compile("^Added action .+!$"),
            Pattern.compile("^Added condition .+!$"),
            Pattern.compile("^Please provide a sound namespace key$"),
            Pattern.compile("^Please enter coordinates, separated by spaces$"),
            Pattern.compile("^\n\n.+\nPlease use the chat to provide the value you wish to set\\.\n.*$")
    );

    @Unique
    private static final Pattern CHAT_INPUT_PATTERN = Pattern.compile("^\n\n.+\nPlease use the chat to provide the value you wish to set\\.\n.*$");

    @Inject(
            method = "handleSystemChat",
            at = @At("HEAD"),
            cancellable = true
    )
    public void onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (!Queue.INSTANCE.isActive()) return;

        String message = packet.content().getString();
        if (message == null) return;

        if (CHAT_INPUT_PATTERN.matcher(message).matches()) {
            InputUtils.INSTANCE.handleInputType(InputUtils.Type.CHAT);
        }

        if (HIDDEN_MESSAGES.stream().anyMatch(p -> p.matcher(message).matches())) {
            ci.cancel();
        }
    }

}