package me.chrr.camerapture.block;

import com.mojang.serialization.MapCodec;
import me.chrr.camerapture.Camerapture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class PictureFrameBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final Identifier ID = Camerapture.id("picture_frame");
    public static final ResourceKey<Block> KEY = ResourceKey.create(Registries.BLOCK, ID);

    public static final MapCodec<PictureFrameBlock> CODEC = simpleCodec(PictureFrameBlock::new);

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    /// How far the frame stands off the wall, in blocks.
    public static final double FRAME_THICKNESS = 0.0625;

    // Thin slab shapes for each direction (1/16 thick on the wall face), used when there's no block
    // entity to read a size from — these are exactly the width-1 height-1 case.
    private static final VoxelShape NORTH_SHAPE = Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0);
    private static final VoxelShape SOUTH_SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);
    private static final VoxelShape EAST_SHAPE = Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public PictureFrameBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public PictureFrameBlock() {
        this(Properties.of()
                .setId(KEY)
                .mapColor(MapColor.NONE)
                .noCollision()
                .noOcclusion()
                .instabreak()
                .sound(SoundType.WOOD)
                .pushReaction(PushReaction.DESTROY));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Raycasts call this for every block along the look vector, every frame, so it must not build a
        // shape per call. The block entity keeps one cached per size; the constants below cover the
        // window where a frame's state exists but its block entity doesn't.
        if (level.getBlockEntity(pos) instanceof PictureFrameBlockEntity blockEntity) {
            return blockEntity.getFrameShape();
        }

        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PictureFrameBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof PictureFrameBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            player.openMenu(blockEntity);
            return InteractionResult.SUCCESS;
        }

        boolean canRotate = Camerapture.CONFIG_MANAGER.getConfig().server.canRotatePictures;
        if (canRotate && !blockEntity.isFixed()) {
            if (!level.isClientSide()) {
                blockEntity.setRotation(blockEntity.getRotation() + 1);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof PictureFrameBlockEntity blockEntity) {
            if (blockEntity.isFixed() && !player.isCreative()) {
                return state; // Don't break if fixed (unless creative)
            }
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                blockEntity.dropItem(serverLevel);
            }
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof PictureFrameBlockEntity blockEntity) {
            blockEntity.dropItem(level);
        }
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (!level.isClientSide() && Camerapture.CONFIG_MANAGER.getConfig().server.checkFramePosition) {
            if (level.getBlockEntity(pos) instanceof PictureFrameBlockEntity blockEntity) {
                if (blockEntity.isFixed()) return;

                if (!canSurvive(state, level, pos)) {
                    if (level instanceof ServerLevel serverLevel) {
                        blockEntity.dropItem(serverLevel);
                    }
                    level.removeBlock(pos, false);
                }
            }
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos backingPos = pos.relative(facing.getOpposite());
        BlockState backingState = level.getBlockState(backingPos);
        return backingState.isFaceSturdy(level, backingPos, facing) || backingState.isSolid();
    }
}
