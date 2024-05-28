package me.chrr.camerapture.entity;

import me.chrr.camerapture.Camerapture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PictureFrameEntity extends ResizableDecorationEntity {
    private static final TrackedData<ItemStack> ITEM_STACK = DataTracker.registerData(PictureFrameEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> GLOWING = DataTracker.registerData(PictureFrameEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FIXED = DataTracker.registerData(PictureFrameEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public PictureFrameEntity(EntityType<? extends PictureFrameEntity> entityType, World world) {
        super(entityType, world);
    }

    public PictureFrameEntity(World world, BlockPos pos, Direction facing) {
        super(Camerapture.PICTURE_FRAME, world);

        this.setAttachmentPos(pos);
        this.setFacing(facing);
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();

        this.getDataTracker().startTracking(ITEM_STACK, ItemStack.EMPTY);
        this.getDataTracker().startTracking(GLOWING, false);
        this.getDataTracker().startTracking(FIXED, false);
    }

    @Override
    public void onBreak(@Nullable Entity entity) {
        this.playSound(SoundEvents.ENTITY_ITEM_FRAME_BREAK, 1f, 1f);
        this.emitGameEvent(GameEvent.BLOCK_CHANGE, entity);

        ItemStack itemStack = this.getItemStack();
        if (itemStack != null) {
            itemStack.setHolder(null);
            this.dropStack(itemStack);
        }
    }

    @Override
    public void onPlace() {
        this.playSound(SoundEvents.ENTITY_ITEM_FRAME_PLACE, 1f, 1f);
    }

    @Nullable
    public ItemStack getItemStack() {
        return this.getDataTracker().get(ITEM_STACK);
    }

    public void setItemStack(ItemStack itemStack) {
        this.getDataTracker().set(ITEM_STACK, itemStack);
    }

    public boolean isPictureGlowing() {
        return this.getDataTracker().get(GLOWING);
    }

    public void setPictureGlowing(boolean glowing) {
        this.getDataTracker().set(GLOWING, glowing);
    }

    public boolean isFixed() {
        return this.getDataTracker().get(FIXED);
    }

    public void setFixed(boolean fixed) {
        this.getDataTracker().set(FIXED, fixed);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);

        if (data.equals(ITEM_STACK)) {
            ItemStack itemStack = getItemStack();
            if (itemStack != null) {
                itemStack.setHolder(this);
            }
        }
    }

    @Override
    public boolean shouldRender(double distance) {
        double d = 16.0;
        d *= 4.0 * getRenderDistanceMultiplier();
        return distance < d * d;
    }

    @Override
    public void move(MovementType movementType, Vec3d movement) {
        if (!this.isFixed()) {
            super.move(movementType, movement);
        }
    }

    @Override
    public void addVelocity(double deltaX, double deltaY, double deltaZ) {
        if (!this.isFixed()) {
            super.addVelocity(deltaX, deltaY, deltaZ);
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.isFixed() && !source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isSourceCreativePlayer()) {
            return false;
        }

        return super.damage(source, amount);
    }

    @Override
    public boolean canStayAttached() {
        return this.isFixed() || super.canStayAttached();
    }

    public void resize(ResizeDirection direction, boolean shrink) {
        boolean success = switch (direction) {
            case UP, DOWN -> tryAddHeight(shrink ? -1 : 1);
            case LEFT, RIGHT -> tryAddWidth(shrink ? -1 : 1);
        };

        if (success) {
            int i = shrink ? 1 : -1;
            switch (direction) {
                case DOWN -> setAttachmentPos(getAttachmentPos().offset(Direction.UP, i));
                case LEFT -> setAttachmentPos(getAttachmentPos().offset(getFacing().rotateYCounterclockwise(), i));
            }
        }
    }

    private boolean tryAddWidth(int n) {
        int width = getFrameWidth();
        if (width >= 1 - n && width <= 16 - n) {
            setFrameWidth(width + n);
            return true;
        } else {
            return false;
        }
    }

    private boolean tryAddHeight(int n) {
        int height = getFrameHeight();
        if (height >= 1 - n && height <= 16 - n) {
            setFrameHeight(height + n);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.getFacing().getId(), this.getBlockPos());
    }

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        this.setFacing(Direction.byId(packet.getEntityData()));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        ItemStack itemStack = this.getItemStack();
        if (itemStack != null) {
            nbt.put("Item", itemStack.writeNbt(new NbtCompound()));
        }

        nbt.putBoolean("PictureGlowing", this.isPictureGlowing());
        nbt.putBoolean("Fixed", this.isFixed());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        NbtCompound nbtCompound = nbt.getCompound("Item");
        if (nbtCompound != null && !nbtCompound.isEmpty()) {
            ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
            if (itemStack.isEmpty()) {
                Camerapture.LOGGER.warn("unable to load item from: {}", nbtCompound);
            }

            this.setItemStack(itemStack);
        }

        this.setPictureGlowing(nbt.getBoolean("PictureGlowing"));
        this.setFixed(nbt.getBoolean("Fixed"));
    }

    @Override
    public ItemStack getPickBlockStack() {
        ItemStack itemStack = this.getItemStack();
        if (itemStack == null) {
            return ItemStack.EMPTY;
        } else {
            return itemStack.copy();
        }
    }

    @Override
    public boolean hasCustomName() {
        return getItemStack() != null && getItemStack().hasCustomName();
    }

    @Nullable
    @Override
    public Text getCustomName() {
        return hasCustomName() ? Objects.requireNonNull(getItemStack()).getName() : null;
    }

    public enum ResizeDirection {
        UP, DOWN, LEFT, RIGHT
    }
}
