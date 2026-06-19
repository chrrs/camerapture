package me.chrr.camerapture.entity;

import me.chrr.camerapture.Camerapture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/// This is mostly a copy of {@link HangingEntity}, but modified
/// to make it resizable. This could've been done using a superclass, but
/// there's other behaviour I didn't need for the picture frames, so this
/// ended up being less work.
public abstract class ResizableDecorationEntity extends Entity {
    public static final double THICKNESS = 1.0 / 16.0;

    private static final EntityDataAccessor<Integer> FRAME_WIDTH = SynchedEntityData.defineId(ResizableDecorationEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FRAME_HEIGHT = SynchedEntityData.defineId(ResizableDecorationEntity.class, EntityDataSerializers.INT);

    private Direction facing = Direction.SOUTH;
    private BlockPos attachmentPos;

    private int obstructionCheckCounter = 0;

    public ResizableDecorationEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FRAME_WIDTH, 1);
        builder.define(FRAME_HEIGHT, 1);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        if (data.equals(FRAME_WIDTH) || data.equals(FRAME_HEIGHT)) {
            updateBoundingBox();
        }
    }

    public void resetObstructionCheckCounter() {
        this.obstructionCheckCounter = 0;
    }

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel level && Camerapture.CONFIG_MANAGER.getConfig().server.checkFramePosition) {
            if (this.obstructionCheckCounter++ == 100) {
                this.obstructionCheckCounter = 0;
                if (!this.canStayAttached() && !this.isRemoved()) {
                    this.discard();
                    this.onBreak(level, null);
                }
            }
        }
    }

    public BlockPos getAttachmentPos() {
        return attachmentPos;
    }

    public void setAttachmentPos(BlockPos attachmentPos) {
        this.attachmentPos = attachmentPos;
        updateBoundingBox();
    }

    public Direction getNearestViewDirection() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;

        this.setYRot((float) (this.facing.get2DDataValue() * 90));
        this.yRotO = this.getYRot();

        updateBoundingBox();
    }

    public int getFrameWidth() {
        return this.getEntityData().get(FRAME_WIDTH);
    }

    public void setFrameWidth(int width) {
        this.getEntityData().set(FRAME_WIDTH, width);
        updateBoundingBox();
    }

    public int getFrameHeight() {
        return this.getEntityData().get(FRAME_HEIGHT);
    }

    public void setFrameHeight(int height) {
        this.getEntityData().set(FRAME_HEIGHT, height);
        updateBoundingBox();
    }

    protected void updateBoundingBox() {
        if (this.facing == null) {
            return;
        }

        // Find the center of the top-left piece.
        Vec3 center = Vec3.atCenterOf(attachmentPos);
        center = center.subtract(new Vec3(facing.getStepX(), 0, facing.getStepZ()).scale(0.5 - THICKNESS / 2));

        this.setPosRaw(center.x, center.y, center.z);

        // Then, we expand into the full frame.
        Direction parallel = facing.getCounterClockWise();
        if (facing.getAxis() == Direction.Axis.Z) {
            Vec3 p1 = center.subtract(parallel.getStepX() * 0.5, 0.5, THICKNESS / 2);
            Vec3 p2 = p1.add(parallel.getStepX() * getFrameWidth(), getFrameHeight(), THICKNESS);
            this.setBoundingBox(new AABB(p1, p2));
        } else {
            Vec3 p1 = center.subtract(THICKNESS / 2, 0.5, parallel.getStepZ() * 0.5);
            Vec3 p2 = p1.add(THICKNESS, getFrameHeight(), parallel.getStepZ() * getFrameWidth());
            this.setBoundingBox(new AABB(p1, p2));
        }

        resetObstructionCheckCounter();
    }

    @Override
    public Direction getDirection() {
        return getNearestViewDirection();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public boolean canStayAttached() {
        if (!Camerapture.CONFIG_MANAGER.getConfig().server.checkFramePosition) {
            return true;
        } else if (!this.level().noCollision(this)) {
            return false;
        } else {
            BlockPos blockPos = this.attachmentPos.relative(this.facing.getOpposite());
            Direction direction = this.facing.getCounterClockWise();
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

            for (int x = 0; x < this.getFrameWidth(); ++x) {
                for (int y = 0; y < this.getFrameHeight(); ++y) {
                    mutable.set(blockPos).move(direction, x).move(Direction.UP, y);
                    BlockState blockState = this.level().getBlockState(mutable);

                    //noinspection deprecation
                    if (!blockState.isSolid() && !DiodeBlock.isDiode(blockState)) {
                        return false;
                    }
                }
            }

            return this.level()
                    .getEntities(this, this.getBoundingBox(), (entity) ->
                            entity instanceof HangingEntity || entity instanceof ResizableDecorationEntity)
                    .isEmpty();
        }
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (attacker instanceof Player playerEntity) {
            //noinspection deprecation: let's just copy what Vanilla does for now.
            return !this.level().mayInteract(playerEntity, this.blockPosition())
                    || this.hurtOrSimulate(this.damageSources().playerAttack(playerEntity), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableToBase(source)) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level().isClientSide()) {
                this.kill(level);
                this.markHurt();
                this.onBreak(level, source.getEntity());
            }

            return true;
        }
    }

    @Override
    public void move(MoverType movementType, Vec3 movement) {
        if (this.level() instanceof ServerLevel world && !this.isRemoved() && movement.lengthSqr() > 0.0) {
            this.kill(world);
            this.onBreak(world, null);
        }
    }

    @Override
    public void push(double deltaX, double deltaY, double deltaZ) {
        if (this.level() instanceof ServerLevel world && !this.isRemoved() && deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 0.0) {
            this.kill(world);
            this.onBreak(world, null);
        }
    }

    @Override
    public void setPos(double x, double y, double z) {
        this.attachmentPos = BlockPos.containing(x, y, z);
        this.updateBoundingBox();
        this.needsSync = true;
    }

    @Override
    public float rotate(Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_180 -> this.setFacing(this.getNearestViewDirection().getOpposite());
            case COUNTERCLOCKWISE_90 -> this.setFacing(this.getNearestViewDirection().getCounterClockWise());
            case CLOCKWISE_90 -> this.setFacing(this.getNearestViewDirection().getClockWise());
        }

        return switch (rotation) {
            case CLOCKWISE_180 -> this.getYRot() + 180.0F;
            case COUNTERCLOCKWISE_90 -> this.getYRot() + 90.0F;
            case CLOCKWISE_90 -> this.getYRot() + 270.0F;
            default -> this.getYRot();
        };
    }

    @Override
    public ItemEntity spawnAtLocation(ServerLevel level, ItemStack stack, float yOffset) {
        Vec3 center = getBoundingBox().getCenter();

        ItemEntity itemEntity = new ItemEntity(level,
                center.x + this.getNearestViewDirection().getStepX() * 0.15F,
                center.y + yOffset,
                center.z + this.getNearestViewDirection().getStepZ() * 0.15F,
                stack);

        itemEntity.setDefaultPickUpDelay();
        this.level().addFreshEntity(itemEntity);
        return itemEntity;
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("block_pos", BlockPos.CODEC, attachmentPos);

        output.store("facing", Direction.CODEC, this.getNearestViewDirection());
        output.putInt("width", this.getFrameWidth());
        output.putInt("height", this.getFrameHeight());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        BlockPos blockPos = input.read("block_pos", BlockPos.CODEC).orElse(null);
        if (blockPos == null || !blockPos.closerThan(this.blockPosition(), 16.0)) {
            Camerapture.LOGGER.error("hanging entity at invalid position: {}", blockPos);
        } else {
            this.attachmentPos = blockPos;
        }

        this.setFacing(input.read("facing", Direction.CODEC).orElse(Direction.NORTH));
        this.setFrameWidth(input.getIntOr("width", 1));
        this.setFrameHeight(input.getIntOr("height", 1));

        updateBoundingBox();
    }

    @Override
    public float mirror(Mirror mirror) {
        return this.rotate(mirror.getRotation(this.getNearestViewDirection()));
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
    }

    public abstract void onPlace();

    public abstract void onBreak(ServerLevel level, @Nullable Entity entity);
}