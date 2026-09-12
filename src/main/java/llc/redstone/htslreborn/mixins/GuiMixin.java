package llc.redstone.htslreborn.mixins;

//? if >=26.1.2 {
/*import llc.redstone.htslreborn.utils.MenuUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///? } else {
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
//? }

@Mixin(Gui.class)
public class GuiMixin {
    //? if >=26.1.2 {
    /*@Inject(method="setScreen", at=@At("RETURN"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen == null) {
            // The menu was closed, player is back in the game
            return;
        }

        MenuUtils.INSTANCE.onScreenOpen(screen);
    }
    *///? }
}
