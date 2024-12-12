package me.chrr.camerapture.render;

import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.entity.ResizableDecorationEntity;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.util.UUID;

public class PictureFrameEntityRenderer extends EntityRenderer<PictureFrameEntity> {
    public static final double DISTANCE_FROM_WALL = 0.01;

    public PictureFrameEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(PictureFrameEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        Vec3d offset = this.getPositionOffset(entity, tickDelta).negate();
        matrices.translate(offset.x, offset.y, offset.z);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        matrices.translate(0.5 - entity.getFrameWidth() / 2.0, -0.5 + entity.getFrameHeight() / 2.0, 0.0);

        MinecraftClient client = MinecraftClient.getInstance();
        boolean shouldRenderOutline = !this.dispatcher.gameOptions.hudHidden
                && CameraItem.find(client.player, true) == null
                && client.crosshairTarget instanceof EntityHitResult hitResult
                && hitResult.getEntity() == entity;

        if (shouldRenderOutline) {
            renderOutline(matrices, vertexConsumers, entity.getFrameWidth(), entity.getFrameHeight());
        }

        matrices.translate(0.0, 0.0, (ResizableDecorationEntity.THICKNESS - DISTANCE_FROM_WALL) / 2.0);

        UUID pictureId = null;
        ItemStack stack = entity.getItemStack();
        if (stack != null) {
            PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
            if (pictureData != null) {
                pictureId = pictureData.id();
            }
        }

        if (pictureId == null) {
            renderErrorText(matrices, vertexConsumers, light);
        } else {
            RemotePicture picture = ClientPictureStore.getInstance().getServerPicture(pictureId);
            if (picture == null || picture.getStatus() == RemotePicture.Status.ERROR) {
                // Picture failed to load.
                renderErrorText(matrices, vertexConsumers, light);
            } else if (picture.getStatus() == RemotePicture.Status.FETCHING) {
                // Picture is still fetching.
                renderFetching(matrices, vertexConsumers, light);
            } else {
                // Picture should be rendered.
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f * entity.getRotation()));
                renderPicture(matrices, vertexConsumers, picture, entity, light);
            }
        }

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    public void renderPicture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, RemotePicture picture, PictureFrameEntity entity, int light) {
        // Find the rendered width and height of the picture.
        float scale = getPictureScale(picture, entity);
        float width = picture.getWidth() * scale;
        float height = picture.getHeight() * scale;

        // Origin is in the center of the entity, so we find the corners
        float x1 = -width / 2f;
        float x2 = width / 2f;
        float y1 = -height / 2f;
        float y2 = height / 2f;

        // If the picture is glowing, we render as if it were text. This avoids
        // the shading based on the normals, as text is always drawn as-is.
        RenderLayer renderLayer = entity.isPictureGlowing()
                ? RenderLayer.getText(picture.getTextureIdentifier())
                : RenderLayer.getEntityCutout(picture.getTextureIdentifier());
        VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);

        MatrixStack.Entry matrix = matrices.peek();
        Matrix4f matrix4f = matrix.getPositionMatrix();
        Matrix3f matrix3f = matrix.getNormalMatrix();

        int effectiveLight = entity.isPictureGlowing() ? 0xff : light;
        buffer.vertex(matrix4f, x1, y1, 0f).color(0xffffffff).texture(1f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix3f, 0f, 0f, 1f).next();
        buffer.vertex(matrix4f, x1, y2, 0f).color(0xffffffff).texture(1f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix3f, 0f, 0f, 1f).next();
        buffer.vertex(matrix4f, x2, y2, 0f).color(0xffffffff).texture(0f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix3f, 0f, 0f, 1f).next();
        buffer.vertex(matrix4f, x2, y1, 0f).color(0xffffffff).texture(0f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix3f, 0f, 0f, 1f).next();
    }

    /// Calculate the picture scale so it fits inside the frame.
    private static float getPictureScale(RemotePicture picture, PictureFrameEntity entity) {
        float pictureWidth = picture.getWidth();
        float pictureHeight = picture.getHeight();

        // If the picture is on its side, we flip width and height.
        if (entity.getRotation() % 2 == 1) {
            pictureWidth = picture.getHeight();
            pictureHeight = picture.getWidth();
        }

        // Calculate the width and height to fit inside the frame.
        float scaledWidth = entity.getFrameWidth() / pictureWidth;
        float scaleHeight = entity.getFrameHeight() / pictureHeight;

        return Math.min(scaledWidth, scaleHeight);
    }

    public void renderOutline(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float frameWidth, float frameHeight) {
        VoxelShape shape = VoxelShapes.cuboid(0.0, 0.0, 0.0, frameWidth, frameHeight, ResizableDecorationEntity.THICKNESS);
        WorldRenderer.drawShapeOutline(matrices, vertexConsumers.getBuffer(RenderLayer.getLines()), shape, -frameWidth / 2, -frameHeight / 2, -ResizableDecorationEntity.THICKNESS / 2f, 0f, 0f, 0f, 102f / 255f, true);
    }

    public void renderFetching(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.scale(-1f / 4f / 16f, -1f / 4f / 16f, 1f / 4f / 16f);
        String loading = LoadingDisplay.get(System.currentTimeMillis());
        Text fetching = Text.translatable("text.camerapture.fetching_picture");
        drawCenteredText(getTextRenderer(), fetching, 0f, -getTextRenderer().fontHeight - 0.5f, 0xffffffff, matrices, vertexConsumers, light);
        drawCenteredText(getTextRenderer(), Text.literal(loading), 0f, 0.5f, 0xff808080, matrices, vertexConsumers, light);
    }

    public void renderErrorText(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.scale(-1f / 4f / 16f, -1f / 4f / 16f, 1f / 4f / 16f);
        Text text = Text.translatable("text.camerapture.fetching_failed").formatted(Formatting.RED);
        drawCenteredText(getTextRenderer(), text, 0f, -getTextRenderer().fontHeight / 2f, 0xffffffff, matrices, vertexConsumers, light);
    }

    private void drawCenteredText(TextRenderer textRenderer, Text text, float x, float y, int color, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        float width = textRenderer.getWidth(text);
        textRenderer.draw(text, x - width / 2f, y, color, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x7f000000, light);
    }

    @Override
    public Identifier getTexture(PictureFrameEntity entity) {
        return null;
    }

    @Override
    public Vec3d getPositionOffset(PictureFrameEntity entity, float tickDelta) {
        Vector3d extra = entity.getFacing().getRotationQuaternion().transform(new Vector3d(((float) entity.getFrameWidth() - 1f) / 2f, 0, -entity.getFrameHeight() + 2));
        return new Vec3d(entity.getFacing().getOffsetX() * 0.3f + extra.x, -0.25f + extra.y, entity.getFacing().getOffsetZ() * 0.3f + extra.z);
    }

    @Override
    protected boolean hasLabel(PictureFrameEntity entity) {
        if (MinecraftClient.isHudEnabled() && entity.hasCustomName()) {
            // Ideally, we'd use `this.dispatcher.targetedEntity`, but that doesn't work for some reason.
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.crosshairTarget instanceof EntityHitResult hitResult && hitResult.getEntity() == entity) {
                double d = this.dispatcher.getSquaredDistanceToCamera(entity);
                float f = entity.isSneaky() ? 32f : 64f;
                return d < f * f;
            }

        }

        return false;
    }
}