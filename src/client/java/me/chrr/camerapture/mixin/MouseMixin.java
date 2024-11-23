package me.chrr.camerapture.mixin;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.chrr.camerapture.CameraptureClient.MAX_ZOOM;
import static me.chrr.camerapture.CameraptureClient.MIN_ZOOM;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(D)V"), cancellable = true)
    public void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // If we have an active camera, scroll to zoom instead.
        if (Camerapture.hasActiveCamera(this.client.player)) {
            CameraptureClient.zoomLevel += (float) vertical / 4f;
            CameraptureClient.zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, CameraptureClient.zoomLevel));

            ci.cancel();
        }
    }
}
