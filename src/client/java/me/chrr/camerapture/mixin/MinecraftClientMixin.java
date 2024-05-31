package me.chrr.camerapture.mixin;

import me.chrr.camerapture.Camerapture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow
    @Nullable
    public ClientPlayerEntity player;

    @Inject(method = "handleBlockBreaking", at = @At(value = "HEAD"), cancellable = true)
    private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
        // Stop accidentally breaking any blocks when taking a photo.
        if (breaking && Camerapture.hasActiveCamera(player)) {
            ci.cancel();
        }
    }
}
