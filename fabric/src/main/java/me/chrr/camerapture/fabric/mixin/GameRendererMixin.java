package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow
    public abstract Minecraft getMinecraft();

    /// We need to notify the picture taker when the render tick ends.
    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRenderTickEnd(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        PictureTaker.getInstance().renderTickEnd();
    }
}
