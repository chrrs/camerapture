package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {
    /// Apply the camera zoom FOV if we have an active camera.
    @Inject(method = "getFieldOfViewModifier", at = @At(value = "HEAD"), cancellable = true)
    public void getFovMultiplier(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        if (CameraItem.find(Minecraft.getInstance().player, true) != null) {
            cir.setReturnValue(PictureTaker.getInstance().getFovModifier());
        }
    }
}
