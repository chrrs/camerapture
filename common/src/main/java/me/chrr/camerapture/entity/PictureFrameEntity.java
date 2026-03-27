package me.chrr.camerapture.entity;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.gui.PictureFrameMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PictureFrameEntity extends ResizableDecorationEntity implements MenuProvider {
    public static final Identifier ID = Camerapture.id("picture_frame");
    public static final ResourceKey<EntityType<?>> KEY = ResourceKey.create(Registries.ENTITY_TYPE, ID);

    private static final EntityDataAccessor<ItemStack> ITEM_STACK = SynchedEntityData.defineId(PictureFrameEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> GLOWING = SynchedEntityData.defineId(PictureFrameEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FIXED = SynchedEntityData.defineId(PictureFrameEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ROTATION = SynchedEntityData.defineId(PictureFrameEntity.class, EntityDataSerializers.INT);

    public PictureFrameEntity(EntityType<? extends PictureFrameEntity> entityType, Level world) {
        super(entityType, world);
    }

    public PictureFrameEntity(Level world, BlockPos pos, Direction facing) {
        super(Camerapture.PICTURE_FRAME, world);

        this.setAttachmentPos(pos);
        this.setFacing(facing);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ITEM_STACK, ItemStack.EMPTY);
        builder.define(GLOWING, false);
        builder.define(FIXED, false);
        builder.define(ROTATION, 0);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        boolean canRotate = Camerapture.CONFIG_MANAGER.getConfig().server.canRotatePictures;

        if (player.isShiftKeyDown()) {
            player.openMenu(this);
            return InteractionResult.SUCCESS;
        } else if (canRotate && !isFixed()) {
            if (!player.level().isClientSide()) {
                setRotation(getRotation() + 1);

                this.playSound(SoundEvents.ITEM_FRAME_ROTATE_ITEM, 1.0F, 1.0F);
                this.gameEvent(GameEvent.BLOCK_CHANGE, player);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onBreak(ServerLevel level, @Nullable Entity entity) {
        this.playSound(SoundEvents.ITEM_FRAME_BREAK, 1f, 1f);
        this.gameEvent(GameEvent.BLOCK_CHANGE, entity);

        ItemStack itemStack = this.getItemStack();
        if (!itemStack.isEmpty()) {
            this.spawnAtLocation(level, itemStack.copy());
        }
    }

    @Override
    public void onPlace() {
        this.playSound(SoundEvents.ITEM_FRAME_PLACE, 1f, 1f);
    }

    public ItemStack getItemStack() {
        return this.getEntityData().get(ITEM_STACK);
    }

    public void setItemStack(ItemStack itemStack) {
        this.getEntityData().set(ITEM_STACK, itemStack);
    }

    public boolean isPictureGlowing() {
        return this.getEntityData().get(GLOWING);
    }

    public void setPictureGlowing(boolean glowing) {
        this.getEntityData().set(GLOWING, glowing);
    }

    public boolean isFixed() {
        return this.getEntityData().get(FIXED);
    }

    public void setFixed(boolean fixed) {
        this.getEntityData().set(FIXED, fixed);
        resetObstructionCheckCounter();
    }

    public int getRotation() {
        return this.getEntityData().get(ROTATION);
    }

    public void setRotation(int rotation) {
        this.getEntityData().set(ROTATION, rotation % 4);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = 16.0;
        d *= 4.0 * getViewScale();
        return distance < d * d;
    }

    @Override
    public void move(MoverType movementType, Vec3 movement) {
        if (!this.isFixed()) {
            super.move(movementType, movement);
        }
    }

    @Override
    public void push(double deltaX, double deltaY, double deltaZ) {
        if (!this.isFixed()) {
            super.push(deltaX, deltaY, deltaZ);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isFixed() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !source.isCreativePlayer()) {
            return false;
        }

        return super.hurtServer(level, source, amount);
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
                case DOWN -> setAttachmentPos(getAttachmentPos().relative(Direction.UP, i));
                case LEFT -> setAttachmentPos(getAttachmentPos().relative(getNearestViewDirection().getCounterClockWise(), i));
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

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity entity) {
        return new ClientboundAddEntityPacket(this, this.getNearestViewDirection().get3DDataValue(), this.blockPosition());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setFacing(Direction.from3DDataValue(packet.getData()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        output.store("item", ItemStack.CODEC, this.getItemStack());
        output.putBoolean("picture_glowing", this.isPictureGlowing());
        output.putBoolean("fixed", this.isFixed());
        output.putInt("rotation", this.getRotation());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        Optional<ItemStack> itemStack = input.read("item", ItemStack.CODEC);
        if (itemStack.isEmpty()) {
            Camerapture.LOGGER.warn("unable to load item for picture frame");
        } else {
            this.setItemStack(itemStack.get());
        }

        this.setPictureGlowing(input.getBooleanOr("picture_glowing", false));
        this.setFixed(input.getBooleanOr("fixed", false));
        this.setRotation(input.getIntOr("rotation", 0));
    }

    @Override
    public ItemStack getPickResult() {
        return this.getItemStack().copy();
    }

    @Override
    public boolean hasCustomName() {
        return getItemStack().get(DataComponents.CUSTOM_NAME) != null;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return hasCustomName() ? getItemStack().getHoverName() : null;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PictureFrameMenu(containerId, this, new ContainerData() {
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
            public int getCount() {
                return 4;
            }
        });
    }

    public enum ResizeDirection {
        UP, DOWN, LEFT, RIGHT
    }
}