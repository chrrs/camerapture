package me.chrr.camerapture.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.chrr.camerapture.screen.PictureSlot;
import me.chrr.camerapture.screen.SizedSlot;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Shadow
    protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY);

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawSlotHighlight(Lnet/minecraft/client/gui/DrawContext;III)V"))
    private void drawSlotHighlight(DrawContext context, int x, int y, int z, Operation<Void> original) {
        if (this.focusedSlot instanceof SizedSlot sizedSlot) {
            context.fillGradient(
                    RenderLayer.getGuiOverlay(),
                    x, y, x + sizedSlot.getWidth(), y + sizedSlot.getHeight(),
                    0x80ffffff, 0x80ffffff, z);
        } else {
            original.call(context, x, y, z);
        }
    }

    @Inject(method = "isPointOverSlot", at = @At(value = "HEAD"), cancellable = true)
    private void isPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (slot instanceof PictureSlot pictureSlot && !pictureSlot.isVisible()) {
            cir.setReturnValue(false);
            cir.cancel();
        } else if (slot instanceof SizedSlot sizedSlot) {
            cir.setReturnValue(isPointWithinBounds(slot.x, slot.y, sizedSlot.getWidth(), sizedSlot.getHeight(), pointX, pointY));
            cir.cancel();
        }
    }
}
