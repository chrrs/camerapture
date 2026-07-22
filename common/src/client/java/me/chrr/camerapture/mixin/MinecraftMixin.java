package me.chrr.camerapture.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    /// While holding an active camera, pick-block is cancelled.
    /// Shift+pick-block toggles landscape/portrait.
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void camerapture$onPickBlock(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (CameraItem.find(client.player, true) == null) {
            return;
        }

        if (client.options.keyShift.isDown()) {
            PictureTaker.getInstance().toggleOrientation();
        }

        ci.cancel();
    }
}
