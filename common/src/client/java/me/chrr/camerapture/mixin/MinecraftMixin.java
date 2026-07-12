package me.chrr.camerapture.mixin;

import me.chrr.camerapture.CameraCaptureControls;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    /// While holding an active camera, middle-click / pick-block cycles the
    /// capture aspect ratio instead of picking a block or entity.
    @Inject(method = "pickBlockOrEntity", at = @At("HEAD"), cancellable = true)
    private void camerapture$onPickBlock(CallbackInfo ci) {
        if (CameraCaptureControls.onPickBlock()) {
            ci.cancel();
        }
    }
}
