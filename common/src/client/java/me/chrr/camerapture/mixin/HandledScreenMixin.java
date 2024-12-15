package me.chrr.camerapture.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.chrr.camerapture.gui.SizedSlot;
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

    /// Draw the slot highlight of sized slots to be bigger according to the specified size.
    ///
    /// Ideally, we'd mixin somewhere else where this isn't a static method, but NeoForge patches
    /// a lot of this function, so we'll mixin into this one instead.
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/screen/slot/Slot;isEnabled()Z", ordinal = 1))
    private boolean drawSlotHighlight(Slot slot, Operation<Boolean> original, @Local(argsOnly = true) DrawContext context) {
        boolean enabled = slot.isEnabled();
        if (enabled && slot instanceof SizedSlot sizedSlot) {
            this.focusedSlot = slot;

            context.fillGradient(RenderLayer.getGuiOverlay(),
                    slot.x, slot.y, slot.x + sizedSlot.getWidth(), slot.y + sizedSlot.getHeight(),
                    0x80ffffff, 0x80ffffff, 0);

            // Return false so the vanilla code doesn't run.
            return false;
        }

        return enabled;
    }

    /// Make sure to return the correct value for sized slots and invisible picture slots.
    @Inject(method = "isPointOverSlot", at = @At(value = "HEAD"), cancellable = true)
    private void isPointOverSlot(Slot slot, double pointX, double pointY, CallbackInfoReturnable<Boolean> cir) {
        if (slot instanceof SizedSlot sizedSlot) {
            cir.setReturnValue(isPointWithinBounds(slot.x, slot.y, sizedSlot.getWidth(), sizedSlot.getHeight(), pointX, pointY));
            cir.cancel();
        }
    }
}
