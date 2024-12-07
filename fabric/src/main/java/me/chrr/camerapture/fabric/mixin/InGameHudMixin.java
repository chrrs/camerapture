package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.gui.CameraViewFinder;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
@Environment(EnvType.CLIENT)
public abstract class InGameHudMixin {
    @Shadow
    public abstract TextRenderer getTextRenderer();

    /// Hide the GUI and draw the camera overlay and viewfinder
    /// when the player is holding an active camera.
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CameraItem.HeldCamera camera = CameraItem.find(MinecraftClient.getInstance().player, true);
        if (camera != null) {
            ci.cancel();
        } else {
            PictureTaker.getInstance().zoomLevel = CameraptureClient.MIN_ZOOM;
            return;
        }

        if (!MinecraftClient.getInstance().options.hudHidden) {
            CameraViewFinder.drawCameraViewFinder(context, getTextRenderer());
        }
    }
}
