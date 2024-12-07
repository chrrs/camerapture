package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    /// Apply the camera zoom FOV if we have an active camera.
    @Inject(method = "getFovMultiplier", at = @At(value = "HEAD"), cancellable = true)
    public void getFovMultiplier(boolean firstPerson, float fovEffectScale, CallbackInfoReturnable<Float> cir) {
        if (CameraItem.find(MinecraftClient.getInstance().player, true) != null) {
            cir.setReturnValue(PictureTaker.getInstance().getFovModifier());
        }
    }
}
