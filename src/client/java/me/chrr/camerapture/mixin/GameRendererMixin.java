package me.chrr.camerapture.mixin;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.picture.PictureTaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    private boolean renderHand;

    @Redirect(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z"))
    private boolean shouldRenderHand(GameRenderer renderer) {
        return !Camerapture.isCameraActive(client.player) && renderHand;
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At(value = "HEAD"), cancellable = true)
    private void shouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (Camerapture.isCameraActive(client.player)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRenderTickEnd(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        PictureTaker.getInstance().renderTickEnd();
    }
}
