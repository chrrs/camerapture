package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.gui.CameraViewFinder;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    public boolean isHidden;

    /// Hide the GUI and draw the camera overlay and viewfinder when the player is holding an active camera.
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;screen()Lnet/minecraft/client/gui/screens/Screen;"), cancellable = true)
    public void onHudRender(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        CameraItem.HeldCamera camera = CameraItem.find(this.minecraft.player, true);
        if (camera == null) {
            PictureTaker.getInstance().zoomLevel = CameraptureClient.MIN_ZOOM;
            return;
        }

        if (!this.isHidden)
            CameraViewFinder.drawCameraViewFinder(graphics, this.minecraft.font);
        ci.cancel();
    }
}
