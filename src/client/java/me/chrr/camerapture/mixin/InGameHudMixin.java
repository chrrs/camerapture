package me.chrr.camerapture.mixin;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.SimpleDateFormat;
import java.util.Date;

@Environment(EnvType.CLIENT)
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Unique
    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy/MM/dd");

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getLastFrameDuration()F"))
    private void onRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (!Camerapture.isCameraActive(client.player)) {
            return;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        drawViewFinder(context, 10, 10, width - 10, height - 10, 2, 30);
        drawViewFinder(context, width / 2 - 20, height / 2 - 20, width / 2 + 20, height / 2 + 20, 1, 10);

        int fh = getTextRenderer().fontHeight;
        int textX = 25;
        int textY = height - 25;

        context.drawText(getTextRenderer(), Text.translatable("text.camerapture.date", SDF_DATE.format(new Date())), textX, textY - fh, 0xffffffff, false);

        int paper = CameraptureClient.paperInInventory();
        if (paper == 0) {
            if (System.currentTimeMillis() % 1000 < 500) {
                int w = getTextRenderer().getWidth(Text.translatable("text.camerapture.no_paper"));
                int x = width / 2 - w / 2;
                int y = height / 2 + 32;
                context.drawText(getTextRenderer(), Text.translatable("text.camerapture.no_paper"), x, y, 0xffff0000, false);
            }
        } else {
            Text text = Text.translatable("text.camerapture.paper_available", paper);
            int w = getTextRenderer().getWidth(text);
            int x = width - 25 - w;
            int y = height - 25 - fh;
            context.drawText(getTextRenderer(), text, x, y, 0xffffffff, false);
        }
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;hudHidden:Z"))
    private boolean isHudHidden(GameOptions options) {
        return Camerapture.isCameraActive(client.player) || options.hudHidden;
    }

    @Unique
    private void drawViewFinder(DrawContext context, int x1, int y1, int x2, int y2, int thickness, int length) {
        context.fill(x1, y1, x1 + length, y1 + thickness, 0xffffffff);
        context.fill(x1, y1, x1 + thickness, y1 + length, 0xffffffff);

        context.fill(x2 - length, y1, x2, y1 + thickness, 0xffffffff);
        context.fill(x2 - thickness, y1, x2, y1 + length, 0xffffffff);

        context.fill(x1, y2 - thickness, x1 + length, y2, 0xffffffff);
        context.fill(x1, y2 - length, x1 + thickness, y2, 0xffffffff);

        context.fill(x2 - length, y2 - thickness, x2, y2, 0xffffffff);
        context.fill(x2 - thickness, y2 - length, x2, y2, 0xffffffff);
    }
}
