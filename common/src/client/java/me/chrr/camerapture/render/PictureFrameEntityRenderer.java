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
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.UUID;

public class PictureFrameEntityRenderer extends EntityRenderer<PictureFrameEntity, PictureFrameEntityRenderer.RenderState> {
    public static final double DISTANCE_FROM_WALL = 0.01;

    public PictureFrameEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(RenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - state.yaw));
        matrices.translate(0.5 - state.frameWidth / 2.0, -0.5 + state.frameHeight / 2.0, 0.0);

        if (state.shouldRenderOutline) {
            renderOutline(matrices, vertexConsumers, state.frameWidth, state.frameHeight);
        }

        matrices.translate(0.0, 0.0, (ResizableDecorationEntity.THICKNESS - DISTANCE_FROM_WALL) / 2.0);

        if (state.pictureId == null) {
            renderErrorText(matrices, vertexConsumers, light);
        } else {
            RemotePicture picture = ClientPictureStore.getInstance().getServerPicture(state.pictureId);
            if (picture == null || picture.getStatus() == RemotePicture.Status.ERROR) {
                // Picture failed to load.
                renderErrorText(matrices, vertexConsumers, light);
            } else if (picture.getStatus() == RemotePicture.Status.FETCHING) {
                // Picture is still fetching.
                renderFetching(matrices, vertexConsumers, light);
            } else {
                // Picture should be rendered.
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f * state.rotation));
                renderPicture(matrices, vertexConsumers, picture, state, light);
            }
        }

        matrices.pop();

        super.render(state, matrices, vertexConsumers, light);
    }

    public void renderPicture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, RemotePicture picture, RenderState state, int light) {
        // Find the rendered width and height of the picture.
        float scale = getPictureScale(picture, state);
        float width = picture.getWidth() * scale;
        float height = picture.getHeight() * scale;

        // Origin is in the center of the entity, so we find the corners
        float x1 = -width / 2f;
        float x2 = width / 2f;
        float y1 = -height / 2f;
        float y2 = height / 2f;

        // If the picture is glowing, we render as if it were a particle. This avoids
        // the shading based on the normals, as particles are always drawn as-is.
        RenderLayer renderLayer = state.isPictureGlowing
                ? RenderLayer.getTranslucentParticle(picture.getTextureIdentifier())
                : RenderLayer.getEntityCutout(picture.getTextureIdentifier());
        VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);

        MatrixStack.Entry matrix = matrices.peek();
        Matrix4f matrix4f = matrix.getPositionMatrix();

        // FIXME: When using shaders, this glows way too bright.
        int effectiveLight = state.isPictureGlowing ? 0xff : light;
        buffer.vertex(matrix4f, x1, y1, 0f).color(0xffffffff).texture(1f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix, 0f, 0f, 1f);
        buffer.vertex(matrix4f, x1, y2, 0f).color(0xffffffff).texture(1f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix, 0f, 0f, 1f);
        buffer.vertex(matrix4f, x2, y2, 0f).color(0xffffffff).texture(0f, 0f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix, 0f, 0f, 1f);
        buffer.vertex(matrix4f, x2, y1, 0f).color(0xffffffff).texture(0f, 1f).overlay(OverlayTexture.DEFAULT_UV).light(effectiveLight).normal(matrix, 0f, 0f, 1f);
    }

    /// Calculate the picture scale so it fits inside the frame.
    private static float getPictureScale(RemotePicture picture, RenderState state) {
        float pictureWidth = picture.getWidth();
        float pictureHeight = picture.getHeight();

        // If the picture is on its side, we flip width and height.
        if (state.rotation % 2 == 1) {
            pictureWidth = picture.getHeight();
            pictureHeight = picture.getWidth();
        }

        // Calculate the width and height to fit inside the frame.
        float scaledWidth = state.frameWidth / pictureWidth;
        float scaleHeight = state.frameHeight / pictureHeight;

        return Math.min(scaledWidth, scaleHeight);
    }

    public void renderOutline(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float frameWidth, float frameHeight) {
        VoxelShape shape = VoxelShapes.cuboid(0.0, 0.0, 0.0, frameWidth, frameHeight, ResizableDecorationEntity.THICKNESS);

        int color = ColorHelper.withAlpha(102, 0xff000000);
        VertexRendering.drawOutline(matrices, vertexConsumers.getBuffer(RenderLayer.getLines()), shape, -frameWidth / 2, -frameHeight / 2, -ResizableDecorationEntity.THICKNESS / 2f, color);
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
        textRenderer.draw(text, x - width / 2f, y, color, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x7f000000, light, false);
    }

    @Override
    protected void renderLabelIfPresent(RenderState state, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.renderLabelIfPresent(state, text, matrices, vertexConsumers, light);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void updateRenderState(PictureFrameEntity entity, RenderState state, float tickDelta) {
        state.pictureId = null;

        ItemStack stack = entity.getItemStack();
        if (stack != null) {
            PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
            if (pictureData != null) {
                state.pictureId = pictureData.id();
            }
        }

        // When hovering, render a "block" outline to make the frame appear as a block.
        MinecraftClient client = MinecraftClient.getInstance();
        state.shouldRenderOutline = !this.dispatcher.gameOptions.hudHidden
                && CameraItem.find(client.player, true) == null
                && client.crosshairTarget instanceof EntityHitResult hitResult
                && hitResult.getEntity() == entity;

        state.isPictureGlowing = entity.isPictureGlowing();
        state.frameWidth = entity.getFrameWidth();
        state.frameHeight = entity.getFrameHeight();
        state.rotation = entity.getRotation();
        state.yaw = entity.getYaw();
    }

    public static class RenderState extends EntityRenderState {
        @Nullable
        public UUID pictureId;
        public boolean isPictureGlowing;
        public boolean shouldRenderOutline;

        public int frameWidth;
        public int frameHeight;
        public int rotation;
        public float yaw;
    }
}