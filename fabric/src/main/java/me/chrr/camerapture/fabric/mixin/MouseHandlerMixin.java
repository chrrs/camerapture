package me.chrr.camerapture.fabric.mixin;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    /// Active camera: scroll zooms, shift+scroll cycles aspect ratio.
    @Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;"), cancellable = true)
    public void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (CameraItem.find(client.player, true) == null) {
            return;
        }

        if (client.options.keyShift.isDown()) {
            if (vertical != 0) {
                PictureTaker.getInstance().cycleAspectRatio(vertical > 0 ? 1 : -1);
            }
        } else {
            PictureTaker.getInstance().zoom((float) (vertical / 4f));
        }

        ci.cancel();
    }
}
