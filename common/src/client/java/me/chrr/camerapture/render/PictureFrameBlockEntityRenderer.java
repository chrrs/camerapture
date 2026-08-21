package me.chrr.camerapture.render;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.block.PictureFrameBlockEntity;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureQuality;
import me.chrr.camerapture.picture.PictureTexture;
import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

public class PictureFrameBlockEntityRenderer implements BlockEntityRenderer<PictureFrameBlockEntity, PictureFrameBlockEntityRenderer.RenderState> {
    public static final double DISTANCE_FROM_WALL = 0.01;
    public static final double FRAME_THICKNESS = 0.0625;

    public PictureFrameBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(PictureFrameBlockEntity blockEntity, RenderState state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);

        CameraptureDebugStats.extractedFrames.incrementAndGet();

        state.renderBox = blockEntity.getRenderBox();
        state.blockEntity = blockEntity;
        state.lastLod = blockEntity.lastLod;
        state.facing = blockEntity.getFacing();
        state.frameWidth = blockEntity.getFrameWidth();
        state.frameHeight = blockEntity.getFrameHeight();
        state.isPictureGlowing = blockEntity.isPictureGlowing();
        state.rotation = blockEntity.getRotation();

        state.pictureId = null;
        ItemStack stack = blockEntity.getItemStack();
        if (stack != null && !stack.isEmpty()) {
            PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
            if (pictureData != null) {
                state.pictureId = pictureData.id();
            }
        }

        Minecraft client = Minecraft.getInstance();
        state.shouldRenderOutline = !client.gui.hud.isHidden()
                && CameraItem.find(client.player, true) == null
                && client.hitResult instanceof BlockHitResult blockHit
                && blockHit.getBlockPos().equals(blockEntity.getBlockPos());
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        CameraptureDebugStats.submittedFrames.incrementAndGet();

        // 1. Frustum Culling: Test the frame's world-space AABB before any store access or computation.
        if (cameraState.cullFrustum != null && !cameraState.cullFrustum.isVisible(state.renderBox)) {
            CameraptureDebugStats.frustumRejected.incrementAndGet();
            return;
        }

        // 2. Screen-space Projected Size & LOD Classification with Hysteresis
        Vec3 camPos = cameraState.pos;
        if (camPos == null) {
            camPos = Minecraft.getInstance().gameRenderer.mainCamera().position();
        }
        double distSq = state.renderBox.distanceToSqr(camPos);
        double distance = Math.max(0.1, Math.sqrt(distSq));

        int viewportHeight = Minecraft.getInstance().getWindow().getHeight();
        double fovDeg = Minecraft.getInstance().options.fov().get();
        double fovRad = Math.toRadians(fovDeg);
        double focalLengthPixels = (viewportHeight / 2.0) / Math.tan(fovRad / 2.0);

        float worldSize = Math.max(state.frameWidth, state.frameHeight);
        float projectedPixels = (float) (worldSize / distance * focalLengthPixels);

        float minPixels = Camerapture.CONFIG_MANAGER.getConfig().client.minimumRenderPixels;
        float fullThreshold = Camerapture.CONFIG_MANAGER.getConfig().client.fullLodPixels;

        PictureLod lod;
        PictureLod prev = state.lastLod;
        if (prev == PictureLod.FULL) {
            if (projectedPixels < minPixels * 0.75f) {
                lod = PictureLod.SKIP;
            } else if (projectedPixels < fullThreshold * 0.85f) {
                lod = PictureLod.THUMBNAIL;
            } else {
                lod = PictureLod.FULL;
            }
        } else if (prev == PictureLod.THUMBNAIL) {
            if (projectedPixels < minPixels * 0.75f) {
                lod = PictureLod.SKIP;
            } else if (projectedPixels >= fullThreshold * 1.15f) {
                lod = PictureLod.FULL;
            } else {
                lod = PictureLod.THUMBNAIL;
            }
        } else {
            if (projectedPixels >= fullThreshold * 1.15f) {
                lod = PictureLod.FULL;
            } else if (projectedPixels >= minPixels * 1.25f) {
                lod = PictureLod.THUMBNAIL;
            } else {
                lod = PictureLod.SKIP;
            }
        }

        if (state.blockEntity != null) {
            state.blockEntity.lastLod = lod;
        }
        state.lod = lod;

        if (lod == PictureLod.SKIP) {
            CameraptureDebugStats.subpixelRejected.incrementAndGet();
            return;
        }

        poseStack.pushPose();

        // Position at center of block, rotate facing outward from wall, and offset for frame dimensions
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.facing.toYRot()));
        poseStack.translate(0.5 - state.frameWidth / 2.0, -0.5 + state.frameHeight / 2.0, 0.5 - (FRAME_THICKNESS / 2.0) + DISTANCE_FROM_WALL);

        if (state.shouldRenderOutline && state.lod == PictureLod.FULL) {
            renderOutline(poseStack, collector, state.frameWidth, state.frameHeight);
        }

        if (state.pictureId == null) {
            if (state.lod == PictureLod.FULL) {
                renderErrorText(poseStack, collector, state.lightCoords);
            } else {
                renderPlaceholderQuad(poseStack, collector, state);
            }
        } else {
            PictureQuality targetQuality = (state.lod == PictureLod.FULL) ? PictureQuality.FULL : PictureQuality.THUMBNAIL;
            RemotePicture picture = ClientPictureStore.getInstance().getPicture(state.pictureId, targetQuality);
            PictureTexture texture = (picture != null) ? picture.getEffectiveTexture(targetQuality) : null;

            if (state.lod == PictureLod.THUMBNAIL) {
                if (texture != null && texture.getStatus() == PictureTexture.Status.SUCCESS) {
                    CameraptureDebugStats.thumbnailRenders.incrementAndGet();
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90f * state.rotation));
                    renderPicture(poseStack, collector, texture, state);
                } else {
                    renderPlaceholderQuad(poseStack, collector, state);
                }
            } else { // FULL LOD
                if (texture == null || texture.getStatus() == PictureTexture.Status.NOT_LOADED || texture.getStatus() == PictureTexture.Status.FETCHING) {
                    renderFetching(poseStack, collector, state.lightCoords);
                } else if (texture.getStatus() == PictureTexture.Status.ERROR) {
                    renderErrorText(poseStack, collector, state.lightCoords);
                } else {
                    CameraptureDebugStats.fullRenders.incrementAndGet();
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90f * state.rotation));
                    renderPicture(poseStack, collector, texture, state);
                }
            }
        }

        poseStack.popPose();
    }

    private void renderPicture(PoseStack poseStack, SubmitNodeCollector collector, PictureTexture texture, RenderState state) {
        float pictureWidth = texture.getWidth();
        float pictureHeight = texture.getHeight();

        if (pictureWidth <= 0 || pictureHeight <= 0) {
            pictureWidth = 1f;
            pictureHeight = 1f;
        }

        if (state.rotation % 2 == 1) {
            float temp = pictureWidth;
            pictureWidth = pictureHeight;
            pictureHeight = temp;
        }

        float scaledWidth = state.frameWidth / pictureWidth;
        float scaleHeight = state.frameHeight / pictureHeight;
        float scale = Math.min(scaledWidth, scaleHeight);

        float width = (state.rotation % 2 == 1 ? texture.getHeight() : texture.getWidth()) * scale;
        float height = (state.rotation % 2 == 1 ? texture.getWidth() : texture.getHeight()) * scale;

        float x1 = -width / 2f;
        float x2 = width / 2f;
        float y1 = -height / 2f;
        float y2 = height / 2f;

        RenderType renderType = state.isPictureGlowing
                ? RenderTypes.text(texture.getTextureIdentifier())
                : RenderTypes.entityCutoutCull(texture.getTextureIdentifier());

        collector.submitCustomGeometry(poseStack, renderType, (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            int effectiveLight = state.isPictureGlowing ? 0xff : state.lightCoords;

            buffer.addVertex(position, x1, y1, 0f).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x1, y2, 0f).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y2, 0f).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y1, 0f).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
        });
    }

    private void renderPlaceholderQuad(PoseStack poseStack, SubmitNodeCollector collector, RenderState state) {
        float x1 = -state.frameWidth / 2f;
        float x2 = state.frameWidth / 2f;
        float y1 = -state.frameHeight / 2f;
        float y2 = state.frameHeight / 2f;
        int color = 0xff2b2b2b;

        collector.submitCustomGeometry(poseStack, RenderTypes.textBackground(), (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            int effectiveLight = state.isPictureGlowing ? 0xff : state.lightCoords;

            buffer.addVertex(position, x1, y1, 0f).setColor(color).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x1, y2, 0f).setColor(color).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y2, 0f).setColor(color).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y1, 0f).setColor(color).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
        });
    }

    private void renderOutline(PoseStack poseStack, SubmitNodeCollector collector, float frameWidth, float frameHeight) {
        VoxelShape shape = Shapes.box(0.0, 0.0, 0.0, frameWidth, frameHeight, FRAME_THICKNESS);

        int color = net.minecraft.util.ARGB.color(102, 0xff000000);
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (matrix, buffer) ->
                shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    Vector3f vector3f = (new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1))).normalize();
                    buffer.addVertex(matrix, (float) (x1 - frameWidth / 2), (float) (y1 - frameHeight / 2), (float) (z1 - FRAME_THICKNESS / 2f)).setColor(color).setNormal(matrix, vector3f).setLineWidth(2.0f);
                    buffer.addVertex(matrix, (float) (x2 - frameWidth / 2), (float) (y2 - frameHeight / 2), (float) (z2 - FRAME_THICKNESS / 2f)).setColor(color).setNormal(matrix, vector3f).setLineWidth(2.0f);
                }));
    }

    private void renderFetching(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Font font = Minecraft.getInstance().font;
        poseStack.scale(-1f / 4f / 16f, -1f / 4f / 16f, 1f / 4f / 16f);
        String loading = LoadingDotsText.get(System.currentTimeMillis());
        Component fetching = Component.translatable("text.camerapture.fetching_picture");
        drawCenteredText(font, fetching, 0f, -font.lineHeight - 0.5f, 0xffffffff, poseStack, collector, light);
        drawCenteredText(font, Component.literal(loading), 0f, 0.5f, 0xff808080, poseStack, collector, light);
    }

    private void renderErrorText(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        Font font = Minecraft.getInstance().font;
        poseStack.scale(-1f / 4f / 16f, -1f / 4f / 16f, 1f / 4f / 16f);
        Component text = Component.translatable("text.camerapture.fetching_failed").withStyle(ChatFormatting.RED);
        drawCenteredText(font, text, 0f, -font.lineHeight / 2f, 0xffffffff, poseStack, collector, light);
    }

    private void drawCenteredText(Font font, Component text, float x, float y, int color, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        float width = font.width(text);
        collector.submitText(poseStack, x - width / 2f, y, Component.translationArg(text).getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, color, 0, 0);
    }

    @Override
    public int getViewDistance() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean shouldRender(PictureFrameBlockEntity blockEntity, Vec3 cameraPos) {
        if (!Camerapture.CONFIG_MANAGER.getConfig().client.distantPictureRendering) {
            return blockEntity.getRenderBox().distanceToSqr(cameraPos) <= 96.0 * 96.0;
        }
        return true;
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    public static AABB getFrameRenderBox(PictureFrameBlockEntity blockEntity) {
        return blockEntity.getRenderBox();
    }

    public static class RenderState extends BlockEntityRenderState {
        public AABB renderBox;
        @Nullable
        public PictureFrameBlockEntity blockEntity;
        public PictureLod lastLod = PictureLod.SKIP;
        public PictureLod lod = PictureLod.SKIP;

        @Nullable
        public UUID pictureId;
        public boolean isPictureGlowing;
        public boolean shouldRenderOutline;
        public int frameWidth;
        public int frameHeight;
        public int rotation;
        public Direction facing;
    }
}
