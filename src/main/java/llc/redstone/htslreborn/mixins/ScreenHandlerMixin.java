package llc.redstone.htslreborn.mixins;

import llc.redstone.htslreborn.accessors.HandledScreenAccessor;
import llc.redstone.htslreborn.ui.FileExplorer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public class ScreenHandlerMixin extends Screen implements HandledScreenAccessor {
    @Shadow
    protected int y;

    @Shadow
    protected int x;

    @Shadow
    protected int backgroundWidth;

    protected ScreenHandlerMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    public void htslreborn$render(DrawContext context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        if (!FileExplorer.inActionGui()) return;
        FileExplorer.getINSTANCE().render(context, mouseX, mouseY, deltaTicks);
    }

    @Inject(method="mouseClicked" , at=@At("HEAD"), cancellable = true)
    public void htslreborn$mouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui()) return;
        if (FileExplorer.getINSTANCE().mouseClicked(click, doubled)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void htslreborn$init(CallbackInfo ci) {
        if (!FileExplorer.inActionGui()) return;
        FileExplorer.setINSTANCE(new FileExplorer());
        FileExplorer.getINSTANCE().init(this.width, this.height);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    public void htslreborn$keyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui()) return;
        if (FileExplorer.getINSTANCE().keyPressed(input)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseDragged(Click click, double offsetX, double offsetY, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui()) return;
        if (FileExplorer.getINSTANCE().mouseDragged(click, offsetX, offsetY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    public void htslreborn$mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        if (!FileExplorer.inActionGui()) return;
        if (FileExplorer.getINSTANCE().mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void htslreborn$close(CallbackInfo ci) {
        if (!FileExplorer.inActionGui()) return;
        FileExplorer.getINSTANCE().close();
    }

    @Override
    public int getXSize() {
        return this.backgroundWidth;
    }

    @Override
    public int getGuiTop() {
        return this.y;
    }

    @Override
    public int getGuiLeft() {
        return this.x;
    }
}
