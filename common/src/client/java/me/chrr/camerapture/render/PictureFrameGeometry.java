package me.chrr.camerapture.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.block.PictureFrameBlock;
import me.chrr.camerapture.picture.PictureTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class PictureFrameGeometry {
    public static final float FRAME_DEPTH =
            (float) PictureFrameBlock.FRAME_THICKNESS;

    public static final float HALF_FRAME_DEPTH =
            FRAME_DEPTH / 2.0F;

    // Outward / player-facing surface
    public static final float FRONT_Z =
            -HALF_FRAME_DEPTH;

    // Wall-facing surface
    public static final float BACK_Z =
            HALF_FRAME_DEPTH;

    // Put photo a hair in front of the physical board.
    public static final float PICTURE_Z =
            FRONT_Z - 0.001F;

    public static final Identifier PICTURE_BACK_TEXTURE = Camerapture.id("textures/block/picture_back.png");

    private PictureFrameGeometry() {
    }

    /// Submits a single rear quad for the frame backing (used for distant decorations and thumbnail LOD).
    public static void submitBackQuad(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float frameWidth,
            float frameHeight,
            int lightCoords
    ) {
        RenderType renderType = RenderTypes.entityCutoutCull(PICTURE_BACK_TEXTURE);
        collector.submitCustomGeometry(poseStack, renderType, (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            float x1 = -frameWidth / 2.0f;
            float x2 = frameWidth / 2.0f;
            float y1 = -frameHeight / 2.0f;
            float y2 = frameHeight / 2.0f;

            // BACK (+Z in local coords, facing wall)
            buffer.addVertex(position, x2, y1, BACK_Z).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y2, BACK_Z).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x1, y2, BACK_Z).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x1, y1, BACK_Z).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);
        });
    }

    /// Submits the full 5-face backing cuboid (rear + top, bottom, left, right edges) for the configured frame.
    public static void submitFullBacking(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float frameWidth,
            float frameHeight,
            int lightCoords
    ) {
        RenderType renderType = RenderTypes.entityCutoutCull(PICTURE_BACK_TEXTURE);
        collector.submitCustomGeometry(poseStack, renderType, (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            float x1 = -frameWidth / 2.0f;
            float x2 = frameWidth / 2.0f;
            float y1 = -frameHeight / 2.0f;
            float y2 = frameHeight / 2.0f;
            float zFront = FRONT_Z;
            float zBack = BACK_Z;

            // 1. BACK (+Z in local coords, facing wall)
            buffer.addVertex(position, x2, y1, zBack).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y2, zBack).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x1, y2, zBack).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x1, y1, zBack).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 0f, 1f);

            // 2. TOP (+Y)
            buffer.addVertex(position, x1, y2, zFront).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 1f, 0f);
            buffer.addVertex(position, x1, y2, zBack).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 1f, 0f);
            buffer.addVertex(position, x2, y2, zBack).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 1f, 0f);
            buffer.addVertex(position, x2, y2, zFront).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, 1f, 0f);

            // 3. BOTTOM (-Y)
            buffer.addVertex(position, x1, y1, zBack).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, -1f, 0f);
            buffer.addVertex(position, x1, y1, zFront).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, -1f, 0f);
            buffer.addVertex(position, x2, y1, zFront).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, -1f, 0f);
            buffer.addVertex(position, x2, y1, zBack).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 0f, -1f, 0f);

            // 4. LEFT (-X)
            buffer.addVertex(position, x1, y1, zBack).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, -1f, 0f, 0f);
            buffer.addVertex(position, x1, y2, zBack).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, -1f, 0f, 0f);
            buffer.addVertex(position, x1, y2, zFront).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, -1f, 0f, 0f);
            buffer.addVertex(position, x1, y1, zFront).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, -1f, 0f, 0f);

            // 5. RIGHT (+X)
            buffer.addVertex(position, x2, y1, zFront).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 1f, 0f, 0f);
            buffer.addVertex(position, x2, y2, zFront).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 1f, 0f, 0f);
            buffer.addVertex(position, x2, y2, zBack).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 1f, 0f, 0f);
            buffer.addVertex(position, x2, y1, zBack).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(lightCoords).setNormal(matrix, 1f, 0f, 0f);
        });
    }

    /// Renders the front photo quad.
    public static void renderPicture(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            PictureTexture texture,
            float frameWidth,
            float frameHeight,
            int rotation,
            boolean isGlowing,
            int lightCoords
    ) {
        float pictureWidth = texture.getWidth();
        float pictureHeight = texture.getHeight();

        if (pictureWidth <= 0 || pictureHeight <= 0) {
            pictureWidth = 1f;
            pictureHeight = 1f;
        }

        if (rotation % 2 == 1) {
            float temp = pictureWidth;
            pictureWidth = pictureHeight;
            pictureHeight = temp;
        }

        float scaledWidth = frameWidth / pictureWidth;
        float scaleHeight = frameHeight / pictureHeight;
        float scale = Math.min(scaledWidth, scaleHeight);

        float width = (rotation % 2 == 1 ? texture.getHeight() : texture.getWidth()) * scale;
        float height = (rotation % 2 == 1 ? texture.getWidth() : texture.getHeight()) * scale;

        float x1 = -width / 2f;
        float x2 = width / 2f;
        float y1 = -height / 2f;
        float y2 = height / 2f;

        RenderType renderType = isGlowing
                ? RenderTypes.text(texture.getTextureIdentifier())
                : RenderTypes.entityCutoutCull(texture.getTextureIdentifier());

        int effectiveLight = isGlowing ? 0x00F000F0 : lightCoords;

        collector.submitCustomGeometry(poseStack, renderType, (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            buffer.addVertex(position, x1, y1, PICTURE_Z).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
            buffer.addVertex(position, x1, y2, PICTURE_Z).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
            buffer.addVertex(position, x2, y2, PICTURE_Z).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
            buffer.addVertex(position, x2, y1, PICTURE_Z).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
        });
    }

    /// Renders the placeholder quad.
    public static void renderPlaceholderQuad(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            float frameWidth,
            float frameHeight,
            boolean isGlowing,
            int lightCoords
    ) {
        float x1 = -frameWidth / 2f;
        float x2 = frameWidth / 2f;
        float y1 = -frameHeight / 2f;
        float y2 = frameHeight / 2f;
        int color = 0xff2b2b2b;
        int effectiveLight = isGlowing ? 0x00F000F0 : lightCoords;

        collector.submitCustomGeometry(poseStack, RenderTypes.textBackground(), (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            buffer.addVertex(position, x1, y1, PICTURE_Z).setColor(color).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
            buffer.addVertex(position, x1, y2, PICTURE_Z).setColor(color).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
            buffer.addVertex(position, x2, y2, PICTURE_Z).setColor(color).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
            buffer.addVertex(position, x2, y1, PICTURE_Z).setColor(color).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, -1f);
        });
    }
}
