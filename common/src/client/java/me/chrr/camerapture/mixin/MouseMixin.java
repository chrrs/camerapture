package me.chrr.camerapture.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @ModifyArgs(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"))
    private void updateMouse(Args args) {
        if (CameraItem.find(MinecraftClient.getInstance().player, true) != null) {
            float modifier = PictureTaker.getInstance().getSensitivityModifier();
            args.setAll((double) args.get(0) * (double) modifier, (double) args.get(1) * (double) modifier);
        }
    }
}
