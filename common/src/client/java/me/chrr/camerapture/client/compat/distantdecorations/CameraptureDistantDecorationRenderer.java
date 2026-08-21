package me.chrr.camerapture.client.compat.distantdecorations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.chrr.camerapture.compat.distantdecorations.CameraptureDistantData;
import me.chrr.camerapture.compat.distantdecorations.CameraptureDistantDecorationProvider;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureQuality;
import me.chrr.camerapture.picture.PictureTexture;
import me.chrr.camerapture.picture.RemotePicture;
import me.chrr.camerapture.render.PictureFrameGeometry;
import me.justbecause.distantdecorations.api.DecorationRecord;
import me.justbecause.distantdecorations.api.DecorationType;
import me.justbecause.distantdecorations.api.client.ClientDecorationRegistry;
import me.justbecause.distantdecorations.api.client.DecorationClientRenderer;
import me.justbecause.distantdecorations.client.spatial.ProjectionMetrics;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class CameraptureDistantDecorationRenderer implements DecorationClientRenderer<CameraptureDistantData> {

    public static final double DISTANCE_FROM_WALL = 0.01;

    public static void init() {
        ClientDecorationRegistry.registerRenderer(new CameraptureDistantDecorationRenderer());
    }

    @Override
    public double cullBelowProjectedPixelSize() {
        return 0.01;
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
        PictureTexture texture = (picture != null) ? picture.getEffectiveTexture(PictureQuality.THUMBNAIL) : null;

        BlockPos anchorPos = record.pos();
        Vec3 cameraPos = camera.position();

        poseStack.pushPose();
        poseStack.translate(anchorPos.getX() - cameraPos.x, anchorPos.getY() - cameraPos.y, anchorPos.getZ() - cameraPos.z);
        poseStack.translate(0.5, 0.5, 0.5);

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - data.facing().toYRot()));
        poseStack.translate(0.5 - data.width() / 2.0, -0.5 + data.height() / 2.0, 0.5 - PictureFrameGeometry.HALF_FRAME_DEPTH + DISTANCE_FROM_WALL);

        // Far-LOD visual footprint scaling: ensure subpixel quad maintains ~1px rasterizable footprint
        double targetMinPixelSize = 1.0;
        double visualScale = 1.0;
        if (projectedPixelSize > 0 && projectedPixelSize < targetMinPixelSize) {
            visualScale = Math.min(16.0, targetMinPixelSize / projectedPixelSize);
            me.justbecause.distantdecorations.telemetry.TelemetryMetrics.clientFarLodScaledRenders++;
        }
        if (visualScale > 1.0) {
            poseStack.scale((float) visualScale, (float) visualScale, 1.0F);
        }

        int lightCoords = 0x00F000F0;

        // Render rear quad for distant decorations
        PictureFrameGeometry.submitBackQuad(poseStack, submitNodeCollector, data.width(), data.height(), lightCoords);

        if (texture != null && texture.getStatus() == PictureTexture.Status.SUCCESS) {
            poseStack.pushPose();
            if (data.rotation() != 0) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F * data.rotation()));
            }
            PictureFrameGeometry.renderPicture(poseStack, submitNodeCollector, texture, data.width(), data.height(), data.rotation(), data.isGlowing(), lightCoords);
            poseStack.popPose();
        } else {
            PictureFrameGeometry.renderPlaceholderQuad(poseStack, submitNodeCollector, data.width(), data.height(), data.isGlowing(), lightCoords);
        }

        poseStack.popPose();
    }
}
