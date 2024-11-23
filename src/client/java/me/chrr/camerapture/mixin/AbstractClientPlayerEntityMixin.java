package me.chrr.camerapture.mixin;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.chrr.camerapture.CameraptureClient.MAX_ZOOM;
import static me.chrr.camerapture.CameraptureClient.MIN_ZOOM;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "getFovMultiplier", at = @At(value = "HEAD"), cancellable = true)
    public void getFovMultiplier(CallbackInfoReturnable<Float> cir) {
        // If we have an active camera, we override the FOV multiplier by the zoom level.
        // We change the FOV exponentially to make it seem like we're linearly zooming in.
        if (Camerapture.hasActiveCamera((PlayerEntity) (Object) this)) {
            float zoomProgress = 1f - (CameraptureClient.zoomLevel - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM);
            cir.setReturnValue(0.1f + 0.9f * (float) Math.pow(zoomProgress, 2.0));
        }
    }
}
