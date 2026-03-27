package me.chrr.camerapture.mixin;

import me.chrr.camerapture.gui.SizedSlot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Shadow
    @Nullable
    protected Slot hoveredSlot;

    @Shadow
    protected abstract boolean isHovering(int x, int y, int width, int height, double pointX, double pointY);

    @ModifyArgs(method = {"extractSlotHighlightBack", "extractSlotHighlightFront"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"))
    public void drawSlotHighlight(Args args) {
        if (this.hoveredSlot instanceof SizedSlot sizedSlot) {
            args.set(4, sizedSlot.getWidth() + 8);
            args.set(5, sizedSlot.getHeight() + 8);
        }
    }

    @Inject(method = "isHovering(Lnet/minecraft/world/inventory/Slot;DD)Z", at = @At(value = "HEAD"), cancellable = true)
    private void isHovering(Slot slot, double mouseX, double mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (slot instanceof SizedSlot sizedSlot) {
            cir.setReturnValue(isHovering(slot.x, slot.y, sizedSlot.getWidth(), sizedSlot.getHeight(), mouseX, mouseY));
            cir.cancel();
        }
    }
}
