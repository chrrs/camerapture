package me.chrr.camerapture.render;

import me.chrr.camerapture.block.DisplayBlock;
import me.chrr.camerapture.block.DisplayBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.RotationAxis;

public class DisplayRenderer implements BlockEntityRenderer<DisplayBlockEntity> {
    public DisplayRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(DisplayBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockState state = entity.getCachedState();

        matrices.push();

        matrices.translate(0.5f, 0.5f, 0.5f);

        float rotation = state.get(DisplayBlock.FACING).rotateYClockwise().asRotation();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotation + 90));

        matrices.translate(0f, 0f, 6.5f / 16f);
        matrices.scale(-1f / 64f, -1f / 64f, 1f / 64f);

        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

//        Text error = Text.translatable("text.camerapture.fetching_failed");
//        drawCenteredText(textRenderer, error, 0f, -textRenderer.fontHeight / 2f, 0xff0000, matrices, vertexConsumers);

        String loading = LoadingDisplay.get(System.currentTimeMillis());
        Text fetching = Text.translatable("text.camerapture.fetching_picture");
        drawCenteredText(textRenderer, fetching, 0f, -textRenderer.fontHeight - 0.5f, 0xffffff, matrices, vertexConsumers);
        drawCenteredText(textRenderer, Text.literal(loading), 0f, 0.5f, 0x808080, matrices, vertexConsumers);


        matrices.pop();
    }

    private void drawCenteredText(TextRenderer textRenderer, Text text, float x, float y, int color, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        float width = textRenderer.getWidth(text);
        textRenderer.draw(text, x - width / 2f, y, color, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x7f000000, 0xf000f0);

    }
}
