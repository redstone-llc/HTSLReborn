package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.queue.Queue;
import llc.redstone.htslreborn.ui.HTSLScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), cancellable = true)
    public void htslreborn$render(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!HTSLScreen.shouldBeVisible()) return;

        HTSLScreen.getINSTANCE().extractRenderState(context, mouseX, mouseY, deltaTicks);

        if (Queue.INSTANCE.isActive() || HTSLScreen.Companion.isBrowsing()) {
            ci.cancel();
        }
    }
}
