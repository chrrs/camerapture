package me.chrr.camerapture.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.gui.CameraViewFinder;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    /// Hide the GUI and draw the camera overlay and viewfinder when the player is holding an active camera.
    @WrapOperation(method = "extractRenderState", at = @At(value = "NEW", target = "(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/renderer/state/gui/GuiRenderState;II)Lnet/minecraft/client/gui/GuiGraphicsExtractor;"))
    public GuiGraphicsExtractor onHudRender(Minecraft minecraft, GuiRenderState guiRenderState, int mouseX, int mouseY, Operation<GuiGraphicsExtractor> original, @Cancellable CallbackInfo ci) {
        GuiGraphicsExtractor graphics = original.call(minecraft, guiRenderState, mouseX, mouseY);

        CameraItem.HeldCamera camera = CameraItem.find(minecraft.player, true);
        if (camera != null) {
            ci.cancel();
        } else {
            PictureTaker.getInstance().zoomLevel = CameraptureClient.MIN_ZOOM;
            return graphics;
        }

        if (!guiRenderState.isHudHidden)
            CameraViewFinder.drawCameraViewFinder(graphics, minecraft.font);

        return graphics;
    }
}
