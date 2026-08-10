package me.chrr.camerapture.neoforge;

import me.chrr.camerapture.block.PictureFrameBlockEntity;
import me.chrr.camerapture.render.PictureFrameBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/// NeoForge frustum-culls block entity render states against `getRenderBoundingBox`, whose default is the
/// anchor's 1x1 block. A frame renders up to 16x16 blocks away from that anchor, so the default box culls
/// wide frames the moment the anchor leaves the frustum. The common renderer can't override this hook —
/// it's a NeoForge-only extension method — so we supply it here.
public class NeoPictureFrameBlockEntityRenderer extends PictureFrameBlockEntityRenderer {
    public NeoPictureFrameBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public AABB getRenderBoundingBox(PictureFrameBlockEntity blockEntity) {
        return getFrameRenderBox(blockEntity);
    }
}
