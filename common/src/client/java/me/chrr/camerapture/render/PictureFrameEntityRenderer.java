package me.chrr.camerapture.render;

import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.entity.ResizableDecorationEntity;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.util.ARGB;
import net.minecraft.core.Direction;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.UUID;

public class PictureFrameEntityRenderer extends EntityRenderer<PictureFrameEntity, PictureFrameEntityRenderer.RenderState> {
    public static final double DISTANCE_FROM_WALL = 0.01;

    public PictureFrameEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void submit(RenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        poseStack.pushPose();

        poseStack.translate(this.getRenderOffset(state).reverse());

        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.translate(0.5 - state.frameWidth / 2.0, -0.5 + state.frameHeight / 2.0, 0.0);

        if (state.shouldRenderOutline) {
            renderOutline(poseStack, collector, state.frameWidth, state.frameHeight);
        }

        poseStack.translate(0.0, 0.0, (ResizableDecorationEntity.THICKNESS - DISTANCE_FROM_WALL) / 2.0);

        if (state.pictureId == null) {
            renderErrorText(poseStack, collector, state.lightCoords);
        } else {
            RemotePicture picture = ClientPictureStore.getInstance().getServerPicture(state.pictureId);
            if (picture == null || picture.getStatus() == RemotePicture.Status.ERROR) {
                // Picture failed to load.
                renderErrorText(poseStack, collector, state.lightCoords);
            } else if (picture.getStatus() == RemotePicture.Status.FETCHING) {
                // Picture is still fetching.
                renderFetching(poseStack, collector, state.lightCoords);
            } else {
                // Picture should be rendered.
                poseStack.mulPose(Axis.ZP.rotationDegrees(90f * state.rotation));
                renderPicture(poseStack, collector, picture, state);
            }
        }

        poseStack.popPose();

        super.submit(state, poseStack, collector, cameraState);
    }

    public void renderPicture(PoseStack poseStack, SubmitNodeCollector collector, RemotePicture picture, RenderState state) {
        // Find the rendered width and height of the picture.
        float scale = getPictureScale(picture, state);
        float width = picture.getWidth() * scale;
        float height = picture.getHeight() * scale;

        // Origin is in the center of the entity, so we find the corners
        float x1 = -width / 2f;
        float x2 = width / 2f;
        float y1 = -height / 2f;
        float y2 = height / 2f;

        // If the picture is glowing, we render as if it were text. This avoids
        // the shading based on the normals, as text is always drawn as-is.
        RenderType renderType = state.isPictureGlowing
                ? RenderTypes.text(picture.getTextureIdentifier())
                : RenderTypes.entityCutoutCull(picture.getTextureIdentifier());

        collector.submitCustomGeometry(poseStack, renderType, (matrix, buffer) -> {
            Matrix4f position = matrix.pose();
            int effectiveLight = state.isPictureGlowing ? 0xff : state.lightCoords;

            buffer.addVertex(position, x1, y1, 0f).setColor(0xffffffff).setUv(1f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x1, y2, 0f).setColor(0xffffffff).setUv(1f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y2, 0f).setColor(0xffffffff).setUv(0f, 0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
            buffer.addVertex(position, x2, y1, 0f).setColor(0xffffffff).setUv(0f, 1f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(effectiveLight).setNormal(matrix, 0f, 0f, 1f);
        });
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

    public void renderOutline(PoseStack poseStack, SubmitNodeCollector collector, float frameWidth, float frameHeight) {
        VoxelShape shape = Shapes.box(0.0, 0.0, 0.0, frameWidth, frameHeight, ResizableDecorationEntity.THICKNESS);

        int color = ARGB.color(102, 0xff000000);
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (matrix, buffer) ->
                shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                    Vector3f vector3f = (new Vector3f((float) (x2 - x1), (float) (y2 - y1), (float) (z2 - z1))).normalize();
                    buffer.addVertex(matrix, (float) (x1 - frameWidth / 2), (float) (y1 - frameHeight / 2), (float) (z1 - ResizableDecorationEntity.THICKNESS / 2f)).setColor(color).setNormal(matrix, vector3f).setLineWidth(2.0f);
                    buffer.addVertex(matrix, (float) (x2 - frameWidth / 2), (float) (y2 - frameHeight / 2), (float) (z2 - ResizableDecorationEntity.THICKNESS / 2f)).setColor(color).setNormal(matrix, vector3f).setLineWidth(2.0f);
                }));
    }

    public void renderFetching(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.scale(-1f / 4f / 16f, -1f / 4f / 16f, 1f / 4f / 16f);
        String loading = LoadingDotsText.get(System.currentTimeMillis());
        Component fetching = Component.translatable("text.camerapture.fetching_picture");
        drawCenteredText(getFont(), fetching, 0f, -getFont().lineHeight - 0.5f, 0xffffffff, poseStack, collector, light);
        drawCenteredText(getFont(), Component.literal(loading), 0f, 0.5f, 0xff808080, poseStack, collector, light);
    }

    public void renderErrorText(PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.scale(-1f / 4f / 16f, -1f / 4f / 16f, 1f / 4f / 16f);
        Component text = Component.translatable("text.camerapture.fetching_failed").withStyle(ChatFormatting.RED);
        drawCenteredText(getFont(), text, 0f, -getFont().lineHeight / 2f, 0xffffffff, poseStack, collector, light);
    }

    private void drawCenteredText(Font font, Component text, float x, float y, int color, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        // FIXME: rendering a text background here causes z-fighting.
        float width = font.width(text);
        collector.submitText(poseStack, x - width / 2f, y, Component.translationArg(text).getVisualOrderText(), false, Font.DisplayMode.NORMAL, light, color, 0, 0);
    }

    @Override
    public Vec3 getRenderOffset(RenderState state) {
        Vector3d extra = state.facing.getRotation().transform(new Vector3d(((float) state.frameWidth - 1f) / 2f, 0, -state.frameHeight + 2));
        return new Vec3(state.facing.getStepX() * 0.3f + extra.x, -0.25f + 1.0f + extra.y, state.facing.getStepZ() * 0.3f + extra.z);
    }

    @Nullable
    @Override
    protected Component getNameTag(PictureFrameEntity entity) {
        return entity.getCustomName();
    }

    @Override
    protected boolean shouldShowName(PictureFrameEntity entity, double squaredDistanceToCamera) {
        return Minecraft.renderNames() && super.shouldShowName(entity, squaredDistanceToCamera);
    }

    @Override
    public RenderState createRenderState() {
        return new RenderState();
    }

    @Override
    public void extractRenderState(PictureFrameEntity entity, RenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);

        state.pictureId = null;

        ItemStack stack = entity.getItemStack();
        if (stack != null) {
            PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
            if (pictureData != null) {
                state.pictureId = pictureData.id();
            }
        }

        // When hovering, render a "block" outline to make the frame appear as a block.
        Minecraft client = Minecraft.getInstance();
        state.shouldRenderOutline = !this.entityRenderDispatcher.options.hideGui
                && CameraItem.find(client.player, true) == null
                && client.hitResult instanceof EntityHitResult hitResult
                && hitResult.getEntity() == entity;

        state.isPictureGlowing = entity.isPictureGlowing();
        state.frameWidth = entity.getFrameWidth();
        state.frameHeight = entity.getFrameHeight();
        state.rotation = entity.getRotation();
        state.yRot = entity.getYRot();
        state.facing = entity.getNearestViewDirection();
    }

    public static class RenderState extends EntityRenderState {
        @Nullable
        public UUID pictureId;
        public boolean isPictureGlowing;
        public boolean shouldRenderOutline;

        public int frameWidth;
        public int frameHeight;
        public int rotation;
        public float yRot;
        public Direction facing;
    }
}