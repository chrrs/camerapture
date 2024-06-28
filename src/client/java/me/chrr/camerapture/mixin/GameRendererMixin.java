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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.21
/*import net.minecraft.client.render.RenderTickCounter;*/

@Environment(EnvType.CLIENT)
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    private boolean renderHand;

    @Shadow
    public abstract void setRenderHand(boolean renderHand);

    @Unique
    boolean shouldveRenderedHand;

    //? if >=1.21 {
    /*@Inject(method = "render", at = @At(value = "HEAD"))
    private void determineRenderHand(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (Camerapture.hasActiveCamera(client.player)) {
            shouldveRenderedHand = renderHand;
            setRenderHand(false);
        }
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void resetRenderHand(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (Camerapture.hasActiveCamera(client.player)) {
            setRenderHand(shouldveRenderedHand);
        }
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At(value = "HEAD"), cancellable = true)
    private void shouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (Camerapture.hasActiveCamera(client.player)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRenderTickEnd(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        PictureTaker.getInstance().renderTickEnd();
    }
    *///?} else {
    @Inject(method = "render", at = @At(value = "HEAD"))
    private void determineRenderHand(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        if (Camerapture.hasActiveCamera(client.player)) {
            shouldveRenderedHand = renderHand;
            setRenderHand(false);
        }
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void resetRenderHand(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        if (Camerapture.hasActiveCamera(client.player)) {
            setRenderHand(shouldveRenderedHand);
        }
    }

    @Inject(method = "shouldRenderBlockOutline", at = @At(value = "HEAD"), cancellable = true)
    private void shouldRenderBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (Camerapture.hasActiveCamera(client.player)) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRenderTickEnd(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        PictureTaker.getInstance().renderTickEnd();
    }
    //?}
}
