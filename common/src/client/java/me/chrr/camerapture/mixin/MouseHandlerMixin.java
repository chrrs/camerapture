package me.chrr.camerapture.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @ModifyArgs(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void updateMouse(Args args) {
        if (CameraItem.find(Minecraft.getInstance().player, true) != null) {
            float modifier = PictureTaker.getInstance().getSensitivityModifier();
            args.setAll((double) args.get(0) * (double) modifier, (double) args.get(1) * (double) modifier);
        }
    }
}
