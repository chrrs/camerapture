package me.chrr.camerapture.entity;

import me.chrr.camerapture.Camerapture;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class ResizableDecorationEntity extends Entity {
    private static final double THICKNESS = 1.0 / 16.0;

    private static final TrackedData<Integer> FRAME_WIDTH = DataTracker.registerData(ResizableDecorationEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> FRAME_HEIGHT = DataTracker.registerData(ResizableDecorationEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private Direction facing = Direction.SOUTH;
    private BlockPos attachmentPos;

    public ResizableDecorationEntity(EntityType<?> type, World world) {
        super(type, world);
    }

    @Override
    protected void initDataTracker() {
        this.getDataTracker().startTracking(FRAME_WIDTH, 1);
        this.getDataTracker().startTracking(FRAME_HEIGHT, 1);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (data.equals(FRAME_WIDTH) || data.equals(FRAME_HEIGHT)) {
            updateBoundingBox();
        }
    }

    public BlockPos getAttachmentPos() {
        return attachmentPos;
    }

    public void setAttachmentPos(BlockPos attachmentPos) {
        this.attachmentPos = attachmentPos;
        updateBoundingBox();
    }

    public Direction getFacing() {
        return facing;
    }

    public void setFacing(Direction facing) {
        this.facing = facing;

        this.setYaw(this.facing.getHorizontal() * 90f);
        this.prevYaw = this.getYaw();

        updateBoundingBox();
    }

    public int getFrameWidth() {
        return this.getDataTracker().get(FRAME_WIDTH);
    }

    public void setFrameWidth(int width) {
        this.getDataTracker().set(FRAME_WIDTH, width);
        updateBoundingBox();
    }

    public int getFrameHeight() {
        return this.getDataTracker().get(FRAME_HEIGHT);
    }

    public void setFrameHeight(int height) {
        this.getDataTracker().set(FRAME_HEIGHT, height);
        updateBoundingBox();
    }

    protected void updateBoundingBox() {
        if (this.facing == null) {
            return;
        }

        Vec3d center = attachmentPos.toCenterPos();
        center = center.subtract(new Vec3d(facing.getOffsetX(), 0, facing.getOffsetZ()).multiply(0.5 - THICKNESS / 2));

        this.setPos(center.x, center.y, center.z);

        Direction parallel = facing.rotateYCounterclockwise();
        if (facing.getAxis() == Direction.Axis.Z) {
            Vec3d p1 = center.subtract(parallel.getOffsetX() * 0.5, 0.5, THICKNESS / 2);
            Vec3d p2 = p1.add(parallel.getOffsetX() * getFrameWidth(), getFrameHeight(), THICKNESS);
            this.setBoundingBox(new Box(p1, p2));
        } else {
            Vec3d p1 = center.subtract(THICKNESS / 2, 0.5, parallel.getOffsetZ() * 0.5);
            Vec3d p2 = p1.add(THICKNESS, getFrameHeight(), parallel.getOffsetZ() * getFrameWidth());
            this.setBoundingBox(new Box(p1, p2));
        }
    }

    @Override
    public Direction getHorizontalFacing() {
        return getFacing();
    }

    @Override
    public boolean canHit() {
        return true;
    }

    public boolean canStayAttached() {
        // FIXME: implement
        return true;
    }

    @Override
    public boolean handleAttack(Entity attacker) {
        if (attacker instanceof PlayerEntity playerEntity) {
            return !this.getWorld().canPlayerModifyAt(playerEntity, this.getBlockPos())
                    || this.damage(this.getDamageSources().playerAttack(playerEntity), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if (!this.isRemoved() && !this.getWorld().isClient) {
                this.kill();
                this.scheduleVelocityUpdate();
                this.onBreak(source.getAttacker());
            }

            return true;
        }
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if (!this.getWorld().isClient && !this.isRemoved() && movement.lengthSquared() > 0.0) {
            this.kill();
            this.onBreak(null);
        }
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        if (!this.getWorld().isClient && !this.isRemoved() && deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 0.0) {
            this.kill();
            this.onBreak(null);
        }
    }

    @Override
    public void setPosition(double x, double y, double z) {
        this.attachmentPos = BlockPos.ofFloored(x, y, z);
        this.updateBoundingBox();
        this.velocityDirty = true;
    }

    @Override
    public float applyRotation(BlockRotation rotation) {
        switch (rotation) {
            case CLOCKWISE_180 -> this.setFacing(this.getFacing().getOpposite());
            case COUNTERCLOCKWISE_90 -> this.setFacing(this.getFacing().rotateYCounterclockwise());
            case CLOCKWISE_90 -> this.setFacing(this.getFacing().rotateYClockwise());
        }

        return switch (rotation) {
            case CLOCKWISE_180 -> this.getYaw() + 180.0F;
            case COUNTERCLOCKWISE_90 -> this.getYaw() + 90.0F;
            case CLOCKWISE_90 -> this.getYaw() + 270.0F;
            default -> this.getYaw();
        };
    }

    @Override
    public ItemEntity dropStack(ItemStack stack, float yOffset) {
        Vec3d center = getBoundingBox().getCenter();
        ItemEntity itemEntity = new ItemEntity(this.getWorld(),
                center.x + this.getFacing().getOffsetX() * 0.15F, center.y + yOffset, center.z + this.getFacing().getOffsetZ() * 0.15F, stack);

        itemEntity.setToDefaultPickupDelay();
        this.getWorld().spawnEntity(itemEntity);
        return itemEntity;
    }

    @Override
    protected boolean shouldSetPositionOnLoad() {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        nbt.putInt("TileX", attachmentPos.getX());
        nbt.putInt("TileY", attachmentPos.getY());
        nbt.putInt("TileZ", attachmentPos.getZ());

        nbt.putByte("Facing", (byte) this.getFacing().getId());
        nbt.putInt("Width", this.getFrameWidth());
        nbt.putInt("Height", this.getFrameHeight());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        BlockPos blockPos = new BlockPos(nbt.getInt("TileX"), nbt.getInt("TileY"), nbt.getInt("TileZ"));
        if (!blockPos.isWithinDistance(this.getBlockPos(), 16.0)) {
            Camerapture.LOGGER.error("hanging entity at invalid position: {}", blockPos);
        } else {
            this.attachmentPos = blockPos;
        }

        this.setFacing(Direction.byId(nbt.getByte("Facing")));
        this.setFrameWidth(nbt.getInt("Width"));
        this.setFrameHeight(nbt.getInt("Height"));

        updateBoundingBox();
    }

    @Override
    public float applyMirror(BlockMirror mirror) {
        return this.applyRotation(mirror.getRotation(this.getFacing()));
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
    }

    @Override
    public void calculateDimensions() {
    }

    public abstract void onPlace();

    public abstract void onBreak(@Nullable Entity entity);
}
