package me.chrr.camerapture.render;

import com.mojang.serialization.MapCodec;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
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

        RemotePicture picture = ClientPictureStore.getInstance().getPicture(pictureData.id(), me.chrr.camerapture.picture.PictureQuality.FULL);
        return picture.getFull().getStatus() == me.chrr.camerapture.picture.PictureTexture.Status.SUCCESS;
    }

    @Override
    public void submit(@Nullable UUID data, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (data == null) {
            return;
        }

        RemotePicture picture = ClientPictureStore.getInstance().getPicture(data, me.chrr.camerapture.picture.PictureQuality.FULL);
        me.chrr.camerapture.picture.PictureTexture texture = picture.getEffectiveTexture(me.chrr.camerapture.picture.PictureQuality.FULL);
        if (texture.getStatus() != me.chrr.camerapture.picture.PictureTexture.Status.SUCCESS) {
            return;
        }

        poseStack.pushPose();

        // Scale down to a 14x14 rectangle in the middle of the slot.
        poseStack.translate(0f, 0f, 0.5f);
        poseStack.translate(1f / 16f, 1f / 16f, 0f);
        poseStack.scale(14 / 16f, 14 / 16f, 14 / 16f);

        // Scale down the picture to fit.
        if (texture.getWidth() > texture.getHeight()) {
            float height = (float) texture.getHeight() / (float) texture.getWidth();
            poseStack.translate(0f, (1f - height) / 2f, 0f);
            poseStack.scale(1f, height, 1f);
        } else {
            float width = (float) texture.getWidth() / (float) texture.getHeight();
            poseStack.translate((1f - width) / 2f, 0f, 0f);
            poseStack.scale(width, 1f, 1f);
        }

        // Render the picture.
        RenderType renderLayer = RenderTypes.entityCutout(texture.getTextureIdentifier());
        collector.submitCustomGeometry(poseStack, renderLayer, (matrix, buffer) -> {
            Matrix4f matrix4f = matrix.pose();
            buffer.addVertex(matrix4f, 1f, 0f, 0f).setColor(0xffffffff).setUv(1f, 1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(matrix4f, 1f, 1f, 0f).setColor(0xffffffff).setUv(1f, 0f).setOverlay(overlay).setLight(light).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(matrix4f, 0f, 1f, 0f).setColor(0xffffffff).setUv(0f, 0f).setOverlay(overlay).setLight(light).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(matrix4f, 0f, 0f, 0f).setColor(0xffffffff).setUv(0f, 1f).setOverlay(overlay).setLight(light).setNormal(matrix, 0f, 0f, 1f);
        });

        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> vertices) {
        vertices.accept(new Vector3f(1f, 0f, 0f));
        vertices.accept(new Vector3f(1f, 1f, 0f));
        vertices.accept(new Vector3f(0f, 1f, 0f));
        vertices.accept(new Vector3f(0f, 0f, 0f));
    }

    @Override
    public @Nullable UUID extractArgument(ItemStack stack) {
        PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
        return pictureData != null ? pictureData.id() : null;
    }

    public static class Unbaked implements SpecialModelRenderer.Unbaked<UUID> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<UUID> bake(BakingContext context) {
            return new PictureItemRenderer();
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
