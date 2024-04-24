package me.chrr.camerapture.entity;

import me.chrr.camerapture.Camerapture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class PictureEntity extends AbstractDecorationEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final TrackedData<ItemStack> ITEM_STACK = DataTracker.registerData(PictureEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Integer> WIDTH_PIXELS = DataTracker.registerData(PictureEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> HEIGHT_PIXELS = DataTracker.registerData(PictureEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> GLOWING = DataTracker.registerData(PictureEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public PictureEntity(EntityType<? extends PictureEntity> entityType, World world) {
        super(entityType, world);
    }

    public PictureEntity(World world, BlockPos pos, Direction facing) {
        super(Camerapture.PICTURE_ENTITY, world, pos);
        this.setFacing(facing);
    }

    @Override
    protected void initDataTracker() {
        this.getDataTracker().startTracking(ITEM_STACK, ItemStack.EMPTY);
        this.getDataTracker().startTracking(WIDTH_PIXELS, 16);
        this.getDataTracker().startTracking(HEIGHT_PIXELS, 16);
        this.getDataTracker().startTracking(GLOWING, false);
    }

    public void setWidthPixels(int width) {
        this.getDataTracker().set(WIDTH_PIXELS, width);
    }

    public void setHeightPixels(int height) {
        this.getDataTracker().set(HEIGHT_PIXELS, height);
    }

    @Override
    public int getWidthPixels() {
        return this.getDataTracker().get(WIDTH_PIXELS);
    }

    @Override
    public int getHeightPixels() {
        return this.getDataTracker().get(HEIGHT_PIXELS);
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

    public boolean getPictureGlowing() {
        return this.getDataTracker().get(GLOWING);
    }

    public void setPictureGlowing(boolean glowing) {
        this.getDataTracker().set(GLOWING, glowing);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        if (data.equals(ITEM_STACK)) {
            ItemStack itemStack = getItemStack();
            if (itemStack != null) {
                itemStack.setHolder(this);
            }
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.facing.getId(), this.getDecorationBlockPos());
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

        nbt.putByte("Facing", (byte) this.facing.getId());
        nbt.putInt("Width", this.getWidthPixels());
        nbt.putInt("Height", this.getHeightPixels());
        nbt.putBoolean("PictureGlowing", this.getPictureGlowing());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        NbtCompound nbtCompound = nbt.getCompound("Item");
        if (nbtCompound != null && !nbtCompound.isEmpty()) {
            ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
            if (itemStack.isEmpty()) {
                LOGGER.warn("unable to load item from: {}", nbtCompound);
            }

            this.setItemStack(itemStack);
        }

        this.setFacing(Direction.byId(nbt.getByte("Facing")));

        if (nbt.contains("Width")) {
            this.setWidthPixels(nbt.getInt("Width"));
        }

        if (nbt.contains("Height")) {
            this.setHeightPixels(nbt.getInt("Height"));
        }

        this.setPictureGlowing(nbt.getBoolean("PictureGlowing"));
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
}
