package me.chrr.camerapture.mixin;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(BuiltinModelItemRenderer.class)
public class BuiltInModelItemRendererMixin {
    /// Render picture items inside our inventory. Note that this only gets called
    /// on an item with parent model `builtin/entity`.
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    private void renderItem(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, CallbackInfo ci) {
        if (stack.isOf(Camerapture.PICTURE)) {
            UUID data = CameraptureClient.PICTURE_ITEM_RENDERER.getData(stack);
            CameraptureClient.PICTURE_ITEM_RENDERER.render(data, matrices, vertexConsumers, light, overlay);
            ci.cancel();
        }
    }
}
