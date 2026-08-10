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

    // Thin slab shapes for each direction (1/16 thick on the wall face)
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
        Direction facing = state.getValue(FACING);
        int width = 1;
        int height = 1;

        if (level.getBlockEntity(pos) instanceof PictureFrameBlockEntity blockEntity) {
            width = blockEntity.getFrameWidth();
            height = blockEntity.getFrameHeight();
        }

        double thickness = 0.0625;
        double minX, minY = 0.0, minZ;
        double maxX, maxY = (double) height, maxZ;

        switch (facing) {
            case SOUTH -> {
                minX = -0.5;
                maxX = (double) width - 0.5;
                minZ = 0.0;
                maxZ = thickness;
            }
            case EAST -> {
                minX = 0.0;
                maxX = thickness;
                minZ = 0.5 - (double) width;
                maxZ = 0.5;
            }
            case WEST -> {
                minX = 1.0 - thickness;
                maxX = 1.0;
                minZ = -0.5;
                maxZ = (double) width - 0.5;
            }
            default -> { // NORTH
                minX = 0.5 - (double) width;
                maxX = 0.5;
                minZ = 1.0 - thickness;
                maxZ = 1.0;
            }
        }

        return Block.box(
                Math.max(-256.0, minX * 16.0),
                Math.max(-256.0, minY * 16.0),
                Math.max(-256.0, minZ * 16.0),
                Math.min(256.0, maxX * 16.0),
                Math.min(256.0, maxY * 16.0),
                Math.min(256.0, maxZ * 16.0)
        );
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
