package me.chrr.camerapture.render;

import com.mojang.blaze3d.systems.RenderSystem;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

//? if <1.21
/*import org.joml.Matrix3f;*/

public class PictureFrameEntityRenderer extends EntityRenderer<PictureFrameEntity> {
    public PictureFrameEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(PictureFrameEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - yaw));
        matrices.translate(0.5 - entity.getFrameWidth() / 2.0, -0.5 + entity.getFrameHeight() / 2.0, 0);
        matrices.scale(0.0625F, 0.0625F, 0.0625F);

        ItemStack itemStack = entity.getItemStack();
        if (itemStack == null) {
            renderErrorText(matrices, vertexConsumers);
        } else {
            // When hovering, render a "block" outline to make the frame appear as a block.
            MinecraftClient client = MinecraftClient.getInstance();
            if (!this.dispatcher.gameOptions.hudHidden
                    && !Camerapture.hasActiveCamera(client.player)
                    && client.crosshairTarget instanceof EntityHitResult hitResult
                    && hitResult.getEntity() == entity) {
                renderOutline(matrices, vertexConsumers, entity.getFrameWidth() * 16f, entity.getFrameHeight() * 16f);
            }

            PictureItem.PictureData pictureData = PictureItem.getPictureData(itemStack);
            if (pictureData == null) {
                // Picture item has no picture data.
                renderErrorText(matrices, vertexConsumers);
            } else {
                ClientPictureStore.Picture picture = ClientPictureStore.getInstance().getServerPicture(pictureData.id());
                if (picture == null || picture.getStatus() == ClientPictureStore.Status.ERROR) {
                    // Picture failed to load.
                    renderErrorText(matrices, vertexConsumers);
                } else if (picture.getStatus() == ClientPictureStore.Status.FETCHING) {
                    // Picture is still fetching.
                    renderFetching(matrices, vertexConsumers);
                } else {
                    // Picture should be rendered.
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90f * entity.getRotation()));
                    renderPicture(matrices, vertexConsumers, picture, entity.getRotation(),
                            entity.getFrameWidth() * 16f, entity.getFrameHeight() * 16f,
                            entity.isPictureGlowing(), light);
                }
            }
        }

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    public void renderPicture(MatrixStack matrices, VertexConsumerProvider vertexConsumers, ClientPictureStore.Picture picture, int rotation, float frameWidth, float frameHeight, boolean glowing, int light) {
        float pictureWidth = picture.getWidth();
        float pictureHeight = picture.getHeight();

        // If the picture is on its side, we flip width and height.
        if (rotation % 2 == 1) {
            pictureWidth = picture.getHeight();
            pictureHeight = picture.getWidth();
        }

        float scaledWidth = frameWidth / pictureWidth;
        float scaleHeight = frameHeight / pictureHeight;

        float scale = Math.min(scaledWidth, scaleHeight);

        float width = picture.getWidth() * scale;
        float height = picture.getHeight() * scale;

        // Origin is in the center of the entity, so we find the corners
        float x1 = -width / 2f;
        float x2 = width / 2f;
        float y1 = -height / 2f;
        float y2 = height / 2f;

        if (glowing) {
            Tessellator tessellator = Tessellator.getInstance();

            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapProgram);
            RenderSystem.setShaderTexture(0, picture.getIdentifier());

            //? if >=1.21 {
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
            //?} else {
            /*BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
            *///?}

            MatrixStack.Entry matrix = matrices.peek();
            pushGlowingVertex(buffer, matrix, x1, y1, 1f, 1f);
            pushGlowingVertex(buffer, matrix, x1, y2, 1f, 0f);
            pushGlowingVertex(buffer, matrix, x2, y2, 0f, 0f);
            pushGlowingVertex(buffer, matrix, x2, y1, 0f, 1f);

            //? if >=1.20.5 {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
            //?} else
            /*tessellator.draw();*/

            RenderSystem.disableDepthTest();
            RenderSystem.disableBlend();
        } else {
            RenderLayer renderLayer = RenderLayer.getEntityCutout(picture.getIdentifier());
            VertexConsumer buffer = vertexConsumers.getBuffer(renderLayer);

            MatrixStack.Entry matrix = matrices.peek();
            pushCutoutVertex(buffer, matrix, x1, y1, 1f, 1f, light);
            pushCutoutVertex(buffer, matrix, x1, y2, 1f, 0f, light);
            pushCutoutVertex(buffer, matrix, x2, y2, 0f, 0f, light);
            pushCutoutVertex(buffer, matrix, x2, y1, 0f, 1f, light);
        }
    }

    private void pushGlowingVertex(VertexConsumer buffer, MatrixStack.Entry matrix, float x, float y, float u, float v) {
        Matrix4f matrix4f = matrix.getPositionMatrix();

        //? if >=1.21 {
        buffer.vertex(matrix4f, x, y, 0f).color(0xffffffff).texture(u, v).light(0xf000f0).normal(matrix, 0f, 0f, 1f);
        //?} else {
        /*Matrix3f matrix3f = matrix.getNormalMatrix();
        buffer.vertex(matrix4f, x, y, 0f).color(0xffffffff).texture(u, v).light(0xf000f0).normal(matrix3f, 0f, 0f, 1f).next();
        *///?}
    }

    private void pushCutoutVertex(VertexConsumer buffer, MatrixStack.Entry matrix, float x, float y, float u, float v, int light) {
        Matrix4f matrix4f = matrix.getPositionMatrix();

        //? if >=1.21 {
        buffer.vertex(matrix4f, x, y, 0f).color(0xffffffff).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix, 0f, 0f, 1f);
        //?} else {
        /*Matrix3f matrix3f = matrix.getNormalMatrix();
        buffer.vertex(matrix4f, x, y, 0f).color(0xffffffff).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(matrix3f, 0f, 0f, 1f).next();
        *///?}
    }

    public void renderOutline(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float frameWidth, float frameHeight) {
        VoxelShape shape = VoxelShapes.cuboid(0.0, 0.0, 0.0, frameWidth, frameHeight, 1.0);
        WorldRenderer.drawShapeOutline(matrices, vertexConsumers.getBuffer(RenderLayer.getLines()), shape, -frameWidth / 2, -frameHeight / 2, -0.5f, 0f, 0f, 0f, 0.4f, true);
    }

    public void renderFetching(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        matrices.scale(-1f / 4f, -1f / 4f, 1f / 4f);
        String loading = LoadingDisplay.get(System.currentTimeMillis());
        Text fetching = Text.translatable("text.camerapture.fetching_picture");
        drawCenteredText(getTextRenderer(), fetching, 0f, -getTextRenderer().fontHeight - 0.5f, 0xffffff, matrices, vertexConsumers);
        drawCenteredText(getTextRenderer(), Text.literal(loading), 0f, 0.5f, 0x808080, matrices, vertexConsumers);
    }

    public void renderErrorText(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        matrices.scale(-1f / 4f, -1f / 4f, 1f / 4f);
        Text text = Text.translatable("text.camerapture.fetching_failed").formatted(Formatting.RED);
        drawCenteredText(getTextRenderer(), text, 0f, -getTextRenderer().fontHeight / 2f, 0xffffff, matrices, vertexConsumers);
    }

    private void drawCenteredText(TextRenderer textRenderer, Text text, float x, float y, int color, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        float width = textRenderer.getWidth(text);
        textRenderer.draw(text, x - width / 2f, y, color, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x7f000000, 0xf000f0);
    }

    @Override
    public Identifier getTexture(PictureFrameEntity entity) {
        return null;
    }

    @Override
    protected int getBlockLight(PictureFrameEntity entity, BlockPos pos) {
        return entity.isPictureGlowing() ? 15 : super.getBlockLight(entity, pos);
    }

    @Override
    protected boolean hasLabel(PictureFrameEntity entity) {
        // this.dispatcher.targetedEntity should work, but it doesn't.
        if (MinecraftClient.isHudEnabled()
                && entity.hasCustomName()
                && MinecraftClient.getInstance().crosshairTarget instanceof EntityHitResult hitResult
                && hitResult.getEntity() == entity) {
            double d = this.dispatcher.getSquaredDistanceToCamera(entity);
            return d < 64f * 64f;
        } else {
            return false;
        }
    }

    //? if >=1.21 {
    @Override
    protected void renderLabelIfPresent(PictureFrameEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        matrices.translate(-((float) entity.getFrameWidth() - 1f) / 2f, entity.getFrameHeight() - 1f, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180.0F + entity.getYaw()));

        super.renderLabelIfPresent(entity, entity.getCustomName(), matrices, vertexConsumers, light, tickDelta);
        matrices.pop();
    }
    //?} else {
    /*@Override
    protected void renderLabelIfPresent(PictureFrameEntity entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - entity.getYaw()));
        matrices.translate(-((float) entity.getFrameWidth() - 1f) / 2f, entity.getFrameHeight() - 1f, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-180.0F + entity.getYaw()));

        super.renderLabelIfPresent(entity, entity.getCustomName(), matrices, vertexConsumers, light);
        matrices.pop();
    }
    *///?}
}
