package me.chrr.camerapture.mixin;

import me.chrr.camerapture.gui.SizedSlot;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Shadow
    protected abstract boolean isPointWithinBounds(int x, int y, int width, int height, double pointX, double pointY);

    @ModifyArgs(method = {"drawSlotHighlightBack", "drawSlotHighlightFront"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Ljava/util/function/Function;Lnet/minecraft/util/Identifier;IIII)V"))
    public void drawSlotHighlight(Args args) {
        if (this.focusedSlot instanceof SizedSlot sizedSlot) {
            args.set(4, sizedSlot.getWidth() + 8);
            args.set(5, sizedSlot.getHeight() + 8);
        }
    }

    @Inject(method = "isPointOverSlot", at = @At(value = "HEAD"), cancellable = true)
    private void isPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (slot instanceof SizedSlot sizedSlot) {
            cir.setReturnValue(isPointWithinBounds(slot.x, slot.y, sizedSlot.getWidth(), sizedSlot.getHeight(), pointX, pointY));
            cir.cancel();
        }
    }
}
