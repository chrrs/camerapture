package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.gui.CameraViewFinder;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Shadow
    public abstract Font getFont();

    /// Hide the GUI and draw the camera overlay and viewfinder
    /// when the player is holding an active camera.
    @Inject(method = "extractRenderState", at = @At(value = "HEAD"), cancellable = true)
    public void onHudRender(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        CameraItem.HeldCamera camera = CameraItem.find(Minecraft.getInstance().player, true);
        if (camera != null) {
            ci.cancel();
        } else {
            PictureTaker.getInstance().zoomLevel = CameraptureClient.MIN_ZOOM;
            return;
        }

        if (!Minecraft.getInstance().options.hideGui) {
            CameraViewFinder.drawCameraViewFinder(context, getFont());
        }
    }
}
