package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererMixin {
    @Shadow
    public abstract MinecraftClient getClient();

    /// We need to notify the picture taker when the render tick ends.
    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRenderTickEnd(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        PictureTaker.getInstance().renderTickEnd();
    }

    /// Hide the hand when the player is holding an active camera.
    @Inject(method = "renderHand", at = @At(value = "HEAD"), cancellable = true)
    private void onRenderHand(Camera gameCamera, float tickDelta, Matrix4f matrix4f, CallbackInfo ci) {
        CameraItem.HeldCamera camera = CameraItem.find(getClient().player, true);
        if (camera != null) {
            ci.cancel();
        }
    }
}
