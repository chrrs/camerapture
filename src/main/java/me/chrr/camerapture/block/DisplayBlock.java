package me.chrr.camerapture.block;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class DisplayBlock extends HorizontalFacingBlock implements Waterloggable, BlockEntityProvider {
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    public static final float THICKNESS = 1f / 16f;
    public static final VoxelShape NORTH_SHAPE = VoxelShapes.cuboid(1f / 16f, 1f / 16f, 0f, 15f / 16f, 15f / 16f, THICKNESS);
    public static final VoxelShape EAST_SHAPE = VoxelShapes.cuboid(1f - THICKNESS, 1f / 16f, 1f / 16f, 1.0f, 15f / 16f, 15f / 16f);
    public static final VoxelShape SOUTH_SHAPE = VoxelShapes.cuboid(1f / 16f, 1f / 16f, 1f - THICKNESS, 15f / 16f, 15f / 16f, 1.0f);
    public static final VoxelShape WEST_SHAPE = VoxelShapes.cuboid(0.0f, 1f / 16f, 1f / 16f, THICKNESS, 15f / 16f, 15f / 16f);

    public DisplayBlock(Settings settings) {
        super(settings);

        setDefaultState(this.getStateManager()
                .getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(WATERLOGGED, false));
    }

    @SuppressWarnings("deprecation")
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        ItemStack stackInHand = player.getStackInHand(hand);
        DisplayBlockEntity blockEntity = (DisplayBlockEntity) world.getBlockEntity(pos);

        if (blockEntity == null) {
            return ActionResult.PASS;
        }

        if (!player.isSneaking()) {
            if (!blockEntity.isEmpty()) {
                ItemStack stack = blockEntity.removeStack();

                if (stackInHand.isEmpty()) {
                    player.setStackInHand(hand, stack);
                } else {
                    player.getInventory().offerOrDrop(stack);
                }

                return ActionResult.SUCCESS;
            }

            if (DisplayBlockEntity.canInsert(stackInHand)) {
                player.setStackInHand(hand, ItemStack.EMPTY);
                blockEntity.setStack(stackInHand);
                return ActionResult.SUCCESS;
            }
        }

        if (!world.isClient) {
            player.openHandledScreen(blockEntity);
            return ActionResult.CONSUME;
        }

        return ActionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof DisplayBlockEntity display) {
                ItemScatterer.spawn(world, pos, display);
            }

            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(WATERLOGGED);
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> VoxelShapes.fullCube();
        };
    }

    @SuppressWarnings("deprecation")
    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : state.getFluidState();
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = ctx.getSide().getOpposite();
        if (facing.getAxis() == Direction.Axis.Y) {
            return null;
        } else {
            FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());

            return this.getDefaultState()
                    .with(FACING, facing)
                    .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        }
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayBlockEntity(pos, state);
    }
}
