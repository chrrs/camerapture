package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    /// If we have an active camera, scroll to zoom instead.
    @Inject(method = "onMouseScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(D)V"), cancellable = true)
    public void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (CameraItem.find(MinecraftClient.getInstance().player, true) != null) {
            PictureTaker.getInstance().zoom((float) (vertical / 4f));
            ci.cancel();
        }
    }
}
