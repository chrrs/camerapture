package me.chrr.camerapture.block;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.gui.PictureFrameMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PictureFrameBlockEntity extends BlockEntity implements MenuProvider {
    public static final ResourceKey<BlockEntityType<?>> KEY = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Camerapture.id("picture_frame"));

    private ItemStack itemStack = ItemStack.EMPTY;
    private boolean glowing = false;
    private boolean fixed = false;
    private int rotation = 0;
    private int frameWidth = 1;
    private int frameHeight = 1;

    public PictureFrameBlockEntity(BlockPos pos, BlockState state) {
        super(Camerapture.PICTURE_FRAME_BLOCK_ENTITY, pos, state);
    }

    // === Getters and Setters ===

    public ItemStack getItemStack() {
        return this.itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        setChanged();
        syncToClient();
    }

    public boolean isPictureGlowing() {
        return this.glowing;
    }

    public void setPictureGlowing(boolean glowing) {
        this.glowing = glowing;
        setChanged();
        syncToClient();
    }

    public boolean isFixed() {
        return this.fixed;
    }

    public void setFixed(boolean fixed) {
        this.fixed = fixed;
        setChanged();
        syncToClient();
    }

    public int getRotation() {
        return this.rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation % 4;
        setChanged();
        syncToClient();
    }

    public int getFrameWidth() {
        return this.frameWidth;
    }

    public void setFrameWidth(int width) {
        this.frameWidth = Math.max(1, Math.min(16, width));
        setChanged();
        syncToClient();
    }

    public int getFrameHeight() {
        return this.frameHeight;
    }

    public void setFrameHeight(int height) {
        this.frameHeight = Math.max(1, Math.min(16, height));
        setChanged();
        syncToClient();
    }

    public Direction getFacing() {
        return this.getBlockState().getValue(PictureFrameBlock.FACING);
    }

    // === Geometry ===

    /// The outline/interaction shape and the render bounding box are derived from the same local bounds,
    /// so the two can't drift apart. Both are cached behind a single immutable snapshot: getShape() runs
    /// during raycasts and neighbour updates, and the render box is consulted every frame for every
    /// loaded frame, so neither can afford to allocate per call. Published as one volatile reference so
    /// the render thread never observes a half-built value.
    private record Geometry(int key, VoxelShape shape, AABB renderBox) {
    }

    @Nullable
    private volatile Geometry geometry;

    private Geometry geometry() {
        Direction facing = getFacing();
        int key = (facing.ordinal() << 10) | (this.frameWidth << 5) | this.frameHeight;

        Geometry current = this.geometry;
        if (current != null && current.key() == key) {
            return current;
        }

        double thickness = PictureFrameBlock.FRAME_THICKNESS;
        double width = this.frameWidth;
        double height = this.frameHeight;

        // Has to match the transform in PictureFrameBlockEntityRenderer#submit: the frame grows upward
        // from the anchor's bottom face, and sideways toward +X/+Z for south/west and toward -X/-Z for
        // north/east. At width 1 every case is the anchor's own 1x1 face.
        double minX, maxX, minZ, maxZ;
        switch (facing) {
            case SOUTH -> {
                minX = 0.0;
                maxX = width;
                minZ = 0.0;
                maxZ = thickness;
            }
            case EAST -> {
                minX = 0.0;
                maxX = thickness;
                minZ = 1.0 - width;
                maxZ = 1.0;
            }
            case WEST -> {
                minX = 1.0 - thickness;
                maxX = 1.0;
                minZ = 0.0;
                maxZ = width;
            }
            default -> { // NORTH
                minX = 1.0 - width;
                maxX = 1.0;
                minZ = 1.0 - thickness;
                maxZ = 1.0;
            }
        }

        VoxelShape shape = Shapes.box(minX, 0.0, minZ, maxX, height, maxZ);
        AABB renderBox = new AABB(
                worldPosition.getX() + minX, worldPosition.getY(), worldPosition.getZ() + minZ,
                worldPosition.getX() + maxX, worldPosition.getY() + height, worldPosition.getZ() + maxZ
        ).inflate(0.5);

        Geometry computed = new Geometry(key, shape, renderBox);
        this.geometry = computed;
        return computed;
    }

    /// The frame's outline and interaction shape, spanning the full picture.
    public VoxelShape getFrameShape() {
        return geometry().shape();
    }

    /// The world-space area this frame draws into, used to keep it out of the frustum cull.
    public AABB getRenderBox() {
        return geometry().renderBox();
    }

    // === Resize ===

    public void resize(ResizeDirection direction, boolean shrink) {
        int delta = shrink ? -1 : 1;
        switch (direction) {
            case UP, DOWN -> {
                int h = getFrameHeight() + delta;
                if (h >= 1 && h <= 16) setFrameHeight(h);
            }
            case LEFT, RIGHT -> {
                int w = getFrameWidth() + delta;
                if (w >= 1 && w <= 16) setFrameWidth(w);
            }
        }
    }

    public enum ResizeDirection {
        UP, DOWN, LEFT, RIGHT
    }

    // === Drops ===

    public void dropItem(ServerLevel level) {
        if (!itemStack.isEmpty()) {
            Vec3 center = Vec3.atCenterOf(worldPosition);
            ItemEntity itemEntity = new ItemEntity(level, center.x, center.y, center.z, itemStack.copy());
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
        }
    }

    // === Sync ===

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === Serialization ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);

        output.store("item", ItemStack.CODEC, this.itemStack);
        output.putBoolean("picture_glowing", this.glowing);
        output.putBoolean("fixed", this.fixed);
        output.putInt("rotation", this.rotation);
        output.putInt("width", this.frameWidth);
        output.putInt("height", this.frameHeight);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        Optional<ItemStack> itemStack = input.read("item", ItemStack.CODEC);
        this.itemStack = itemStack.orElse(ItemStack.EMPTY);

        this.glowing = input.getBooleanOr("picture_glowing", false);
        this.fixed = input.getBooleanOr("fixed", false);
        this.rotation = input.getIntOr("rotation", 0);

        // Clamp on the way in, so nothing downstream has to defend against a hand-edited size. The
        // geometry below turns these straight into a VoxelShape.
        this.frameWidth = Math.clamp(input.getIntOr("width", 1), 1, 16);
        this.frameHeight = Math.clamp(input.getIntOr("height", 1), 1, 16);
        this.geometry = null;
    }

    // === Menu ===

    @Override
    public Component getDisplayName() {
        if (itemStack.get(DataComponents.CUSTOM_NAME) != null) {
            return itemStack.getHoverName();
        }
        return Component.translatable("block.camerapture.picture_frame");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PictureFrameMenu(containerId, this, new ContainerData() {
            @Override
            public int get(int id) {
                return switch (id) {
                    case 0 -> PictureFrameBlockEntity.this.getFrameWidth();
                    case 1 -> PictureFrameBlockEntity.this.getFrameHeight();
                    case 2 -> PictureFrameBlockEntity.this.isPictureGlowing() ? 1 : 0;
                    case 3 -> PictureFrameBlockEntity.this.isFixed() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int id, int value) {
            }

            @Override
            public int getCount() {
                return 4;
            }
        });
    }
}
