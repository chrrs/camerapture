package me.chrr.camerapture.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @WrapOperation(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void updateMouse(ClientPlayerEntity instance, double x, double y, Operation<Void> original) {
        if (CameraItem.find(MinecraftClient.getInstance().player, true) != null) {
            float modifier = PictureTaker.getInstance().getSensitivityModifier();
            original.call(instance, x * (double) modifier, y * (double) modifier);
        } else {
            original.call(instance, x, y);
        }
    }
}
