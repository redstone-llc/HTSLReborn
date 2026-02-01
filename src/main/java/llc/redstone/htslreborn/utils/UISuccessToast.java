package llc.redstone.htslreborn.utils;

import io.wispforest.owo.Owo;
import io.wispforest.owo.ops.TextOps;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.parsing.UIModelLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

@ApiStatus.Internal
public class UISuccessToast implements Toast {

    private final List<OrderedText> errorMessage;
    private final TextRenderer textRenderer;
    private final int width;

    public UISuccessToast(String message) {
        this.textRenderer = MinecraftClient.getInstance().textRenderer;
        var texts = this.initText(message);
        this.width = Math.min(240, TextOps.width(textRenderer, texts) + 8);
        this.errorMessage = this.wrap(texts);
    }

    public static void report(String message) {
        logErrorsDuringInitialLoad();
        MinecraftClient.getInstance().getToastManager().add(new UISuccessToast(message));
    }

    private static void logErrorsDuringInitialLoad() {
        if (UIModelLoader.hasCompletedInitialLoad()) return;

        var throwable = new Throwable();
        Owo.LOGGER.error(
                "An owo-ui error has occurred during the initial resource reload (on thread {}). This is likely a bug caused by *some* other mod initializing an owo-config screen significantly too early - please report it at https://github.com/wisp-forest/owo-lib/issues",
                Thread.currentThread().getName(),
                throwable
        );
    }

    private Visibility visibility = Visibility.HIDE;

    @Override
    public void update(ToastManager manager, long time) {
        this.visibility = time > 10000 ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public Visibility getVisibility() {
        return this.visibility;
    }

    @Override
    public void draw(DrawContext context, TextRenderer textRenderer, long startTime) {
        var owoContext = OwoUIGraphics.of(context);

        owoContext.fill(0, 0, this.getWidth(), this.getHeight(), 0x77000000);
        owoContext.drawRectOutline(0, 0, this.getWidth(), this.getHeight(), 0xA700FF00);

        int xOffset = this.getWidth() / 2 - this.textRenderer.getWidth(this.errorMessage.get(0)) / 2;
        owoContext.drawTextWithShadow(this.textRenderer, this.errorMessage.get(0), 4 + xOffset, 4, 0xFFFFFFFF);

        for (int i = 1; i < this.errorMessage.size(); i++) {
            owoContext.drawText(this.textRenderer, this.errorMessage.get(i), 4, 4 + i * 11, 0xFFFFFFFF, false);
        }
    }

    @Override
    public int getHeight() {
        return 6 + this.errorMessage.size() * 11;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    private List<Text> initText(String successMessage) {
        final var texts = new ArrayList<Text>();
        texts.add(Text.literal("HTSLReborn success").formatted(Formatting.GREEN));
        texts.add(Text.literal(" "));
        texts.add(Text.literal(successMessage));


        return texts;
    }

    private List<OrderedText> wrap(List<Text> message) {
        var list = new ArrayList<OrderedText>();
        for (var text : message) list.addAll(this.textRenderer.wrapLines(text, this.getWidth() - 8));
        return list;
    }

    @Override
    public Object getType() {
        return Type.VERY_TYPE;
    }

    enum Type {
        VERY_TYPE
    }
}