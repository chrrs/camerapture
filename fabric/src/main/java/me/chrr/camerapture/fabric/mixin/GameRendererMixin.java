package me.chrr.camerapture.fabric.mixin;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /// We need to notify the picture taker when the render tick ends.
    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRenderTickEnd(CallbackInfo ci) {
        PictureTaker.getInstance().renderTickEnd();
    }

    /// Hide the hands when the player is holding an active camera.
    @Inject(method = "renderItemInHand", at = @At(value = "HEAD"), cancellable = true)
    public void onRenderItemInHand(CameraRenderState cameraState, PlayerRenderState playerState, GpuTextureView depthTextureView, CallbackInfo ci) {
        CameraItem.HeldCamera camera = CameraItem.find(minecraft.player, true);
        if (camera != null)
            ci.cancel();
    }
}
