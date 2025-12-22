package me.chrr.camerapture.render;

import com.mojang.serialization.MapCodec;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.UUID;
import java.util.function.Consumer;

public class PictureItemRenderer implements SpecialModelRenderer<UUID> {
    public static boolean canRender(ItemStack stack) {
        PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
        if (pictureData == null) {
            return false;
        }

        RemotePicture picture = ClientPictureStore.getInstance().getServerPicture(pictureData.id());
        return picture.getStatus() == RemotePicture.Status.SUCCESS;
    }

    @Override
    public void render(@Nullable UUID data, ItemDisplayContext displayContext, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int i) {
        if (data == null) {
            return;
        }

        RemotePicture picture = ClientPictureStore.getInstance().getServerPicture(data);

        matrices.push();

        // Scale down to a 14x14 rectangle in the middle of the slot.
        matrices.translate(0f, 0f, 0.5f);
        matrices.translate(1f / 16f, 1f / 16f, 0f);
        matrices.scale(14 / 16f, 14 / 16f, 14 / 16f);

        // Scale down the picture to fit.
        if (picture.getWidth() > picture.getHeight()) {
            float height = (float) picture.getHeight() / (float) picture.getWidth();
            matrices.translate(0f, (1f - height) / 2f, 0f);
            matrices.scale(1f, height, 1f);
        } else {
            float width = (float) picture.getWidth() / (float) picture.getHeight();
            matrices.translate((1f - width) / 2f, 0f, 0f);
            matrices.scale(width, 1f, 1f);
        }

        // Render the picture.
        RenderLayer renderLayer = RenderLayers.entityCutoutNoCull(picture.getTextureIdentifier());
        queue.submitCustom(matrices, renderLayer, (matrix, buffer) -> {
            Matrix4f matrix4f = matrix.getPositionMatrix();
            buffer.vertex(matrix4f, 1f, 0f, 0f).color(0xffffffff).texture(1f, 1f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
            buffer.vertex(matrix4f, 1f, 1f, 0f).color(0xffffffff).texture(1f, 0f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
            buffer.vertex(matrix4f, 0f, 1f, 0f).color(0xffffffff).texture(0f, 0f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
            buffer.vertex(matrix4f, 0f, 0f, 0f).color(0xffffffff).texture(0f, 1f).overlay(overlay).light(light).normal(matrix, 0f, 0f, 1f);
        });

        matrices.pop();
    }

    @Override
    public void collectVertices(Consumer<Vector3fc> vertices) {
        vertices.accept(new Vector3f(1f, 0f, 0f));
        vertices.accept(new Vector3f(1f, 1f, 0f));
        vertices.accept(new Vector3f(0f, 1f, 0f));
        vertices.accept(new Vector3f(0f, 0f, 0f));
    }

    @Override
    public @Nullable UUID getData(ItemStack stack) {
        PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
        return pictureData != null ? pictureData.id() : null;
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<UUID> bake(BakeContext context) {
            return new PictureItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> getCodec() {
            return MAP_CODEC;
        }
    }
}
