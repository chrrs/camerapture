package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.picture.PictureTaker;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
@Environment(EnvType.CLIENT)
public abstract class GameRendererMixin {
    /// We need to notify the picture taker when the render tick ends.
    @Inject(method = "render", at = @At(value = "TAIL"))
    private void onRenderTickEnd(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        PictureTaker.getInstance().renderTickEnd();
    }
}
