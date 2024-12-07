package me.chrr.camerapture.mixin;

import me.chrr.camerapture.item.CameraItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    /// Don't render block outlines when holding an active camera.
    @Inject(method = "shouldRenderBlockOutline", at = @At(value = "HEAD"), cancellable = true)
    public void shouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (CameraItem.find(MinecraftClient.getInstance().player, true) != null) {
            cir.setReturnValue(false);
        }
    }
}
