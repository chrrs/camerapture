package me.chrr.camerapture.render;

import me.chrr.camerapture.block.DisplayBlock;
import me.chrr.camerapture.block.DisplayBlockEntity;
import me.chrr.camerapture.picture.ClientPictureStore;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.UUID;

public class DisplayRenderer implements BlockEntityRenderer<DisplayBlockEntity> {
    public DisplayRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(DisplayBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = entity.getCachedState();
        UUID uuid = entity.getPictureUuid();

        if (uuid == null) {
            return;
        }

        ClientPictureStore.Picture picture = ClientPictureStore.getInstance().getServerPicture(uuid);

        matrices.push();

        matrices.translate(0.5f, 0.5f, 0.5f);
        float rotation = state.get(DisplayBlock.FACING).rotateYClockwise().asRotation();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation + 90));
        matrices.translate(0f, 0f, 6.5f / 16f);

        if (picture.getStatus() == ClientPictureStore.Status.SUCCESS) {
            float frameWidth = entity.getWidth();
            float frameHeight = entity.getHeight();

            float scaledWidth = frameWidth / picture.getWidth();
            float scaleHeight = frameHeight / picture.getHeight();

            float scale = Math.min(scaledWidth, scaleHeight);

            float width = picture.getWidth() * scale;
            float height = picture.getHeight() * scale;

            // Invert X, we've probably rotated the matrix the wrong way but this works too.
            float x1 = -entity.getOffsetX() - width / 2f;
            float x2 = -entity.getOffsetX() + width / 2f;
            float y1 = entity.getOffsetY() - height / 2f;
            float y2 = entity.getOffsetY() + height / 2f;

            MatrixStack.Entry matrix = matrices.peek();
            VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityAlpha(picture.getIdentifier()));

            pushVertex(buffer, matrix, x1, y1, 1f, 1f, light);
            pushVertex(buffer, matrix, x1, y2, 1f, 0f, light);
            pushVertex(buffer, matrix, x2, y2, 0f, 0f, light);
            pushVertex(buffer, matrix, x2, y1, 0f, 1f, light);
        } else {
            matrices.scale(-1f / 64f, -1f / 64f, 1f / 64f);
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            switch (picture.getStatus()) {
                case FETCHING -> {
                    String loading = LoadingDisplay.get(System.currentTimeMillis());
                    Text fetching = Text.translatable("text.camerapture.fetching_picture");
                    drawCenteredText(textRenderer, fetching, 0f, -textRenderer.fontHeight - 0.5f, 0xffffff, matrices, vertexConsumers);
                    drawCenteredText(textRenderer, Text.literal(loading), 0f, 0.5f, 0x808080, matrices, vertexConsumers);
                }
                case ERROR -> {
                    Text error = Text.translatable("text.camerapture.fetching_failed");
                    drawCenteredText(textRenderer, error, 0f, -textRenderer.fontHeight / 2f, 0xff0000, matrices, vertexConsumers);
                }
            }
        }

        matrices.pop();
    }

    private void pushVertex(VertexConsumer buffer, MatrixStack.Entry matrix, float x, float y, float u, float v, int light) {
        Matrix4f matrix4f = matrix.getPositionMatrix();
        Matrix3f normal = matrix.getNormalMatrix();

        buffer.vertex(matrix4f, x, y, 0f)
                .color(0xffffffff)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, 0f, 0f, -1f)
                .next();
    }

    private void drawCenteredText(TextRenderer textRenderer, Text text, float x, float y, int color, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        float width = textRenderer.getWidth(text);
        textRenderer.draw(text, x - width / 2f, y, color, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x7f000000, 0xf000f0);
    }
}
