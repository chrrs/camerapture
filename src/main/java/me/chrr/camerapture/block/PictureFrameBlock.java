package me.chrr.camerapture.block;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;


public class PictureFrameBlock extends FrameBlock {
    public PictureFrameBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(FACING, ctx.getSide().getOpposite());
    }
}
