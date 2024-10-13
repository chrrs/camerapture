package me.chrr.camerapture.render;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.render.*;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;

//? if <1.21
/*import org.joml.Matrix3f;*/

public class PictureItemRenderer implements BuiltinItemRendererRegistry.DynamicItemRenderer {
    private void renderEmptyPicture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        matrices.push();
        matrices.translate(0f, 0f, 0.5f);

        RenderLayer renderLayer = RenderLayer.getEntityCutoutNoCull(Camerapture.id("textures/item/picture.png"));
        VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);
        addQuad(buffer, matrices.peek(), 0f, 0f, 1f, 1f, 0f, overlay, light);

        matrices.pop();
    }


    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
        if (pictureData == null) {
            this.renderEmptyPicture(matrices, vertexConsumers, light, overlay);
            return;
        }

        RemotePicture picture = ClientPictureStore.getInstance().getServerPicture(pictureData.id());
        if (picture.getStatus() != RemotePicture.Status.SUCCESS) {
            this.renderEmptyPicture(matrices, vertexConsumers, light, overlay);
            return;
        }

        matrices.push();

        matrices.translate(0f, 0f, 0.5f);
        matrices.translate(1f / 16f, 1f / 16f, 0f);
        matrices.scale(14 / 16f, 14 / 16f, 14 / 16f);

        if (picture.getWidth() > picture.getHeight()) {
            float height = (float) picture.getHeight() / (float) picture.getWidth();
            matrices.translate(0f, (1f - height) / 2f, 0f);
            matrices.scale(1f, height, 1f);
        } else {
            float width = (float) picture.getWidth() / (float) picture.getHeight();
            matrices.translate((1f - width) / 2f, 0f, 0f);
            matrices.scale(width, 1f, 1f);
        }

        RenderLayer renderLayer = RenderLayer.getEntityCutoutNoCull(picture.getTextureIdentifier());
        VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);
        addQuad(buffer, matrices.peek(), 0f, 0f, 1f, 1f, 0f, overlay, light);

        matrices.pop();
    }

    private void addQuad(VertexConsumer buffer, MatrixStack.Entry matrix, float x1, float y1, float x2, float y2, float z, int overlay, int light) {
        Matrix4f matrix4f = matrix.getPositionMatrix();

        //? if >=1.21 {
        buffer.vertex(matrix4f, x2, y1, z).color(0xffffffff).texture(1f, 1f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
        buffer.vertex(matrix4f, x2, y2, z).color(0xffffffff).texture(1f, 0f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
        buffer.vertex(matrix4f, x1, y2, z).color(0xffffffff).texture(0f, 0f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
        buffer.vertex(matrix4f, x1, y1, z).color(0xffffffff).texture(0f, 1f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
        //?} else {
        /*Matrix3f matrix3f = matrix.getNormalMatrix();
        buffer.vertex(matrix4f, x2, y1, z).color(0xffffffff).texture(1f, 1f).overlay(overlay).light(light).normal(matrix3f, 0f, 0f, 1f).next();
        buffer.vertex(matrix4f, x2, y2, z).color(0xffffffff).texture(1f, 0f).overlay(overlay).light(light).normal(matrix3f, 0f, 0f, 1f).next();
        buffer.vertex(matrix4f, x1, y2, z).color(0xffffffff).texture(0f, 0f).overlay(overlay).light(light).normal(matrix3f, 0f, 0f, 1f).next();
        buffer.vertex(matrix4f, x1, y1, z).color(0xffffffff).texture(0f, 1f).overlay(overlay).light(light).normal(matrix3f, 0f, 0f, 1f).next();
        *///?}
    }
}
