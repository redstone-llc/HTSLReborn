package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.utils.MenuUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;

@Mixin(AbstractContainerScreen.class)
public class ScreenRenderMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        MenuUtils.INSTANCE.render$systemsapi();
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    public void drawSlot(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        MenuUtils.INSTANCE.renderStack$htslreborn(slot.getItem());
    }
}
*/
//?} else {

@Mixin(AbstractContainerScreen.class)
public class ScreenRenderMixin {
    @Inject(method = "render", at=@At("HEAD"))
    public void render(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        MenuUtils.INSTANCE.render$htslreborn();
    }

    @Inject(method="renderSlot", at=@At("RETURN"))
    //? if <1.21.11 {
    /*public void drawSlot(GuiGraphics context, Slot slot, CallbackInfo ci) {
        MenuUtils.INSTANCE.renderStack$htslreborn(slot.getItem());
    }
    *///?} else {
      public void drawSlot(GuiGraphics context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
         MenuUtils.INSTANCE.renderStack$htslreborn(slot.getItem());
     } 
    //?}
}
//?}
