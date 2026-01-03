package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.item.CameraItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.ItemInHandRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /// Hide the hands when the player is holding an active camera.
    @Inject(method = "renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V", at = @At(value = "HEAD"), cancellable = true)
    public void onRenderHands(float tickProgress, PoseStack matrices, SubmitNodeCollector orderedRenderCommandQueue, LocalPlayer player, int light, CallbackInfo ci) {
        CameraItem.HeldCamera camera = CameraItem.find(minecraft.player, true);
        if (camera != null) {
            ci.cancel();
        }
    }
}
