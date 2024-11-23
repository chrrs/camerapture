package me.chrr.camerapture.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.text.SimpleDateFormat;
import java.util.Date;

//? if >=1.21 {
import net.minecraft.client.gui.LayeredDrawer;
import net.minecraft.client.render.RenderTickCounter;
//?} else {
/*import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
*///?}

import static me.chrr.camerapture.CameraptureClient.MAX_ZOOM;
import static me.chrr.camerapture.CameraptureClient.MIN_ZOOM;

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

    //? if >=1.21 {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/LayeredDrawer;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void render(LayeredDrawer instance, DrawContext context, RenderTickCounter tickCounter, Operation<Void> original) {
        boolean hasActiveCamera = Camerapture.hasActiveCamera(client.player);

        // Reset zoom level if no camera is active anymore.
        // FIXME: There should be a better place to set this rather than every frame.
        if (!hasActiveCamera) {
            CameraptureClient.zoomLevel = MIN_ZOOM;
        }

        if (!hasActiveCamera || this.client.options.hudHidden) {
            original.call(instance, context, tickCounter);
        } else {
            drawOverlay(context);
        }
    }
    //?} else {
    /*@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;getLastFrameDuration()F"))
    private void onRender(DrawContext context, float tickDelta, CallbackInfo ci) {
        if (Camerapture.hasActiveCamera(client.player)) {
            drawOverlay(context);
        } else {
            // Reset zoom level if no camera is active anymore.
            // FIXME: There should be a better place to set this rather than every frame.
            CameraptureClient.zoomLevel = CameraptureClient.MIN_ZOOM;
        }
    }

    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;hudHidden:Z"))
    private boolean isHudHidden(GameOptions instance, Operation<Boolean> original) {
        return Camerapture.hasActiveCamera(client.player) || original.call(instance);
    }
    *///?}

    @Unique
    private void drawOverlay(DrawContext context) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        drawViewFinder(context, 10, 10, width - 10, height - 10, 2, 30);
        drawViewFinder(context, width / 2 - 20, height / 2 - 20, width / 2 + 20, height / 2 + 20, 1, 10);

        drawZoomBar(context, width - 10, height / 2 - height / 6, height / 3);

        int fh = getTextRenderer().fontHeight;
        int textX = 25;
        int textY = height - 25;

        context.drawText(getTextRenderer(), Text.translatable("text.camerapture.date", SDF_DATE.format(new Date())), textX, textY - fh, 0xffffffff, false);

        if (!CameraptureClient.canTakePicture()) {
            if (System.currentTimeMillis() % 1000 < 500) {
                int w = getTextRenderer().getWidth(Text.translatable("text.camerapture.no_paper"));
                int x = width / 2 - w / 2;
                int y = height / 2 + 32;
                context.drawText(getTextRenderer(), Text.translatable("text.camerapture.no_paper"), x, y, 0xffff0000, false);
            }
        } else {
            int paper = CameraptureClient.paperInInventory();

            Text text = Text.translatable("text.camerapture.paper_available", paper);
            int w = getTextRenderer().getWidth(text);
            int x = width - 25 - w;
            int y = height - 25 - fh;
            context.drawText(getTextRenderer(), text, x, y, 0xffffffff, false);
        }
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

    @Unique
    private void drawZoomBar(DrawContext context, int x, int y, int height) {
        // Draw the ticks along the whole zoom bar.
        int ticks = height / 10;
        for (int i = 0; i < ticks; i++) {
            int ty = y + (height * i) / (ticks - 1);
            context.fill(x - 6, ty, x, ty + 1, 0xafffffff);
        }

        // Draw a line where the current zoom level is.
        float zoomProgress = 1f - (CameraptureClient.zoomLevel - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM);
        int ty = y + (int) ((float) height * zoomProgress);
        context.fill(x - 10, ty - 1, x, ty + 1, 0xffffffff);

        // Show the current zoom level besides the line.
        String zoomLevel = String.format("%.1fx", CameraptureClient.zoomLevel);
        int textWidth = getTextRenderer().getWidth(zoomLevel);
        context.drawText(getTextRenderer(), zoomLevel, x - 12 - textWidth, ty - 4, 0xffffffff, false);
    }
}
