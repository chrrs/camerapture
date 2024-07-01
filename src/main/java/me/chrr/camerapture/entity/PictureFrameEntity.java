package me.chrr.camerapture.entity;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.screen.PictureFrameScreenHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;


//? if >=1.20.5 {
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.network.EntityTrackerEntry;
import java.util.Optional;
//?}

public class PictureFrameEntity extends ResizableDecorationEntity implements NamedScreenHandlerFactory {
    private static final TrackedData<ItemStack> ITEM_STACK = DataTracker.registerData(PictureFrameEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> GLOWING = DataTracker.registerData(PictureFrameEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FIXED = DataTracker.registerData(PictureFrameEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> ROTATION = DataTracker.registerData(PictureFrameEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public PictureFrameEntity(EntityType<? extends PictureFrameEntity> entityType, World world) {
        super(entityType, world);
    }

    public PictureFrameEntity(World world, BlockPos pos, Direction facing) {
        super(Camerapture.PICTURE_FRAME, world);

        this.setAttachmentPos(pos);
        this.setFacing(facing);
    }

    //? if >=1.20.5 {
    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ITEM_STACK, ItemStack.EMPTY);
        builder.add(GLOWING, false);
        builder.add(FIXED, false);
        builder.add(ROTATION, 0);
    }
    //?} else {
    /*@Override
    protected void initDataTracker() {
        super.initDataTracker();

        this.getDataTracker().startTracking(ITEM_STACK, ItemStack.EMPTY);
        this.getDataTracker().startTracking(GLOWING, false);
        this.getDataTracker().startTracking(FIXED, false);
        this.getDataTracker().startTracking(ROTATION, 0);
    }
    *///?}

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        boolean canRotate = Camerapture.CONFIG_MANAGER.getConfig().server.canRotatePictures;

        if (player.isSneaking()) {
            player.openHandledScreen(this);
            return ActionResult.SUCCESS;
        } else if (canRotate && !isFixed()) {
            if (!player.getWorld().isClient) {
                setRotation(getRotation() + 1);

                this.playSound(SoundEvents.ENTITY_ITEM_FRAME_ROTATE_ITEM, 1.0F, 1.0F);
                this.emitGameEvent(GameEvent.BLOCK_CHANGE, player);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    @Override
    public void onBreak(@Nullable Entity entity) {
        this.playSound(SoundEvents.ENTITY_ITEM_FRAME_BREAK, 1f, 1f);
        this.emitGameEvent(GameEvent.BLOCK_CHANGE, entity);

        ItemStack itemStack = this.getItemStack();
        if (!itemStack.isEmpty()) {
            itemStack.setHolder(null);
            this.dropStack(itemStack);
        }
    }

    @Override
    public void onPlace() {
        this.playSound(SoundEvents.ENTITY_ITEM_FRAME_PLACE, 1f, 1f);
    }

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
        resetObstructionCheckCounter();
    }

    public int getRotation() {
        return this.getDataTracker().get(ROTATION);
    }

    public void setRotation(int rotation) {
        this.getDataTracker().set(ROTATION, rotation % 4);
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);

        if (data.equals(ITEM_STACK)) {
            ItemStack itemStack = getItemStack();
            if (!itemStack.isEmpty()) {
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

        resetObstructionCheckCounter();
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

    //? if >=1.20.5 {
    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
        return new EntitySpawnS2CPacket(this, this.getFacing().getId(), this.getBlockPos());
    }
    //?} else {
    /*@Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, this.getFacing().getId(), this.getBlockPos());
    }
    *///?}

    @Override
    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
        super.onSpawnPacket(packet);
        this.setFacing(Direction.byId(packet.getEntityData()));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);

        ItemStack itemStack = this.getItemStack();
        if (!itemStack.isEmpty()) {
            //? if >=1.20.5 {
            nbt.put("Item", itemStack.encode(getRegistryManager()));
            //?} else
            /*nbt.put("Item", itemStack.writeNbt(new NbtCompound()));*/
        }

        nbt.putBoolean("PictureGlowing", this.isPictureGlowing());
        nbt.putBoolean("Fixed", this.isFixed());
        nbt.putInt("PictureRotation", this.getRotation());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);

        NbtCompound nbtCompound = nbt.getCompound("Item");
        if (nbtCompound != null && !nbtCompound.isEmpty()) {
            //? if >=1.20.5 {
            Optional<ItemStack> itemStack = ItemStack.fromNbt(getRegistryManager(), nbtCompound);
            if (itemStack.isEmpty()) {
                Camerapture.LOGGER.warn("unable to load item from: {}", nbtCompound);
            } else {
                this.setItemStack(itemStack.get());
            }
            //?} else
            /*this.setItemStack(ItemStack.fromNbt(nbtCompound));*/
        }

        this.setPictureGlowing(nbt.getBoolean("PictureGlowing"));
        this.setFixed(nbt.getBoolean("Fixed"));
        this.setRotation(nbt.getInt("PictureRotation"));
    }

    @Override
    public ItemStack getPickBlockStack() {
        return this.getItemStack().copy();
    }

    @Override
    public boolean hasCustomName() {
        //? if >=1.20.5 {
        return getItemStack().get(DataComponentTypes.CUSTOM_NAME) != null;
        //?} else
        /*return getItemStack().hasCustomName();*/
    }

    @Nullable
    @Override
    public Text getCustomName() {
        return hasCustomName() ? getItemStack().getName() : null;
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new PictureFrameScreenHandler(syncId, this, new PropertyDelegate() {
            @Override
            public int get(int id) {
                return switch (id) {
                    case 0 -> PictureFrameEntity.this.getFrameWidth();
                    case 1 -> PictureFrameEntity.this.getFrameHeight();
                    case 2 -> PictureFrameEntity.this.isPictureGlowing() ? 1 : 0;
                    case 3 -> PictureFrameEntity.this.isFixed() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int id, int value) {
                // We aren't setting anything via properties, we're accessing
                // the entity directly, as this simple int-based property system
                // is not enough for our needs.
            }

            @Override
            public int size() {
                return 4;
            }
        });
    }

    public enum ResizeDirection {
        UP, DOWN, LEFT, RIGHT
    }
}
