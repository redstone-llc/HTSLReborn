package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.accessor.HandledScreenAccessor;
import llc.redstone.htslreborn.queue.Queue;
import llc.redstone.htslreborn.ui.HTSLScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class ScreenHandlerMixin extends Screen implements HandledScreenAccessor {
    @Shadow
    protected int topPos;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int imageWidth;

    protected ScreenHandlerMixin(Component title) {
        super(title);
    }

    @Unique
    boolean menuRendered = false;

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    public void htslreborn$render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!HTSLScreen.shouldBeVisible()) {
//            if (menuRendered) FileExplorer.getINSTANCE().resetCursor();
            menuRendered = false;
            return;
        }
        HTSLScreen.getINSTANCE().extractRenderState(graphics, mouseX, mouseY, a);
        menuRendered = true;
        if (Queue.INSTANCE.isActive() || HTSLScreen.Companion.isBrowsing()) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!HTSLScreen.shouldBeVisible()) return;
        HTSLScreen.getINSTANCE().mouseClicked(click, doubled);
        if (Queue.INSTANCE.isActive() || HTSLScreen.Companion.isBrowsing()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void htslreborn$init(CallbackInfo ci) {
        if (!HTSLScreen.shouldBeVisible()) return;
        HTSLScreen.setINSTANCE(new HTSLScreen());
        HTSLScreen.getINSTANCE().init(this.width, this.height);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void htslreborn$keyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
        if (!HTSLScreen.shouldBeVisible()) return;
        if (HTSLScreen.getINSTANCE().keyPressed(input)) {
            cir.setReturnValue(true);
        }

        if (Queue.INSTANCE.isActive()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseDragged(MouseButtonEvent click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (!HTSLScreen.shouldBeVisible()) return;
        if (HTSLScreen.getINSTANCE().mouseDragged(click, offsetX, offsetY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
        if (!HTSLScreen.shouldBeVisible()) return;
        if (HTSLScreen.getINSTANCE().mouseReleased(click)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!HTSLScreen.shouldBeVisible()) return;
        if (HTSLScreen.getINSTANCE().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    public void htslreborn$close(CallbackInfo ci) {
        if (!HTSLScreen.shouldBeVisible()) return;
        HTSLScreen.getINSTANCE().onClose();
    }

    @Override
    public int getXSize() {
        return this.imageWidth;
    }

    @Override
    public int getGuiTop() {
        if (((Object) this) instanceof CreativeModeInventoryScreen) {
            return this.topPos - 28;
        }
        return this.topPos;
    }

    @Override
    public int getGuiLeft() {
        return this.leftPos;
    }
}