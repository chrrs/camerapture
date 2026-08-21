package me.chrr.camerapture.client.compat.distantdecorations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.chrr.camerapture.compat.distantdecorations.CameraptureDistantData;
import me.chrr.camerapture.compat.distantdecorations.CameraptureDistantDecorationProvider;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureQuality;
import me.chrr.camerapture.picture.PictureTexture;
import me.chrr.camerapture.picture.RemotePicture;
import me.justbecause.distantdecorations.api.DecorationRecord;
import me.justbecause.distantdecorations.api.DecorationType;
import me.justbecause.distantdecorations.api.client.ClientDecorationRegistry;
import me.justbecause.distantdecorations.api.client.DecorationClientRenderer;
import me.justbecause.distantdecorations.client.spatial.ProjectionMetrics;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class CameraptureDistantDecorationRenderer implements DecorationClientRenderer<CameraptureDistantData> {

    public static void init() {
        ClientDecorationRegistry.registerRenderer(new CameraptureDistantDecorationRenderer());
    }

    @Override
    public DecorationType<CameraptureDistantData> type() {
        return CameraptureDistantDecorationProvider.TYPE;
    }

    @Override
    public void render(
        DecorationRecord record,
        CameraptureDistantData data,
        Camera camera,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        ProjectionMetrics metrics,
        double projectedPixelSize
    ) {
        if (data.pictureId() == null) {
            return;
        }

        // Strictly request thumbnail quality (32x32) for distant decorations
        RemotePicture picture = ClientPictureStore.getInstance().getPicture(data.pictureId(), PictureQuality.THUMBNAIL);
        PictureTexture texture = picture != null ? picture.getTexture(PictureQuality.THUMBNAIL) : null;

        Vec3 cameraPos = camera.position();
        AABB bounds = record.bounds();
        Vec3 center = bounds.getCenter();

        poseStack.pushPose();
        poseStack.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);

        Direction facing = data.facing();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        if (data.rotation() != 0) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(data.rotation() * 90.0F));
        }

        if (texture != null && texture.getStatus() == PictureTexture.Status.SUCCESS) {
            renderPicture(poseStack, submitNodeCollector, texture, data);
        } else {
            renderPlaceholderQuad(poseStack, submitNodeCollector, data);
        }

        poseStack.popPose();
    }

    private void renderPicture(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        PictureTexture texture,
        CameraptureDistantData data
    ) {
        float picWidth = texture.getWidth();
        float picHeight = texture.getHeight();

        if (picWidth <= 0 || picHeight <= 0) {
            picWidth = 1.0F;
            picHeight = 1.0F;
        }

        if (data.rotation() % 2 == 1) {
            float temp = picWidth;
            picWidth = picHeight;
            picHeight = temp;
        }

        float scaledWidth = (float) data.width() / picWidth;
        float scaledHeight = (float) data.height() / picHeight;
        float scale = Math.min(scaledWidth, scaledHeight);

        float width = (data.rotation() % 2 == 1 ? texture.getHeight() : texture.getWidth()) * scale;
        float height = (data.rotation() % 2 == 1 ? texture.getWidth() : texture.getHeight()) * scale;

        float x1 = -width / 2.0F;
        float x2 = width / 2.0F;
        float y1 = -height / 2.0F;
        float y2 = height / 2.0F;

        RenderType renderType = data.isGlowing()
            ? RenderTypes.text(texture.getTextureIdentifier())
            : RenderTypes.entityCutoutCull(texture.getTextureIdentifier());

        int effectiveLight = data.isGlowing() ? 0x00F000F0 : 0x00F000F0;

        collector.submitCustomGeometry(poseStack, renderType, (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            buffer.addVertex(position, x1, y1, 0.0F).setColor(-1).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
            buffer.addVertex(position, x1, y2, 0.0F).setColor(-1).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
            buffer.addVertex(position, x2, y2, 0.0F).setColor(-1).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
            buffer.addVertex(position, x2, y1, 0.0F).setColor(-1).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
        });
    }

    private void renderPlaceholderQuad(
        PoseStack poseStack,
        SubmitNodeCollector collector,
        CameraptureDistantData data
    ) {
        float x1 = -data.width() / 2.0F;
        float x2 = data.width() / 2.0F;
        float y1 = -data.height() / 2.0F;
        float y2 = data.height() / 2.0F;
        int color = 0xFF2B2B2B;

        collector.submitCustomGeometry(poseStack, RenderTypes.textBackground(), (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            int effectiveLight = 0x00F000F0;
            buffer.addVertex(position, x1, y1, 0.0F).setColor(color).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
            buffer.addVertex(position, x1, y2, 0.0F).setColor(color).setUv(0.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
            buffer.addVertex(position, x2, y2, 0.0F).setColor(color).setUv(1.0F, 1.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
            buffer.addVertex(position, x2, y1, 0.0F).setColor(color).setUv(1.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0.0F, 0.0F, 1.0F);
        });
    }
}
