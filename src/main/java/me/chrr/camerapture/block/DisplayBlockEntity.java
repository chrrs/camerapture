package me.chrr.camerapture.block;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.screen.DisplayScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SidedInventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DisplayBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory, SingleStackInventory, SidedInventory {
    public static final float MAX_DIM = 16f;
    public static final float MAX_OFFSET = 8f;

    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);

    private UUID pictureUuid;

    private float offsetX = 0f;
    private float offsetY = 0f;
    private float width = 3f;
    private float height = 3f;

    public DisplayBlockEntity(BlockPos pos, BlockState state) {
        super(Camerapture.DISPLAY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        if (nbt.contains("picture", NbtElement.COMPOUND_TYPE)) {
            this.setStack(ItemStack.fromNbt(nbt.getCompound("picture")));
        }

        this.offsetX = nbt.contains("offsetX") ? nbt.getFloat("offsetX") : 0f;
        this.offsetY = nbt.contains("offsetY") ? nbt.getFloat("offsetY") : 0f;
        this.width = nbt.contains("width") ? nbt.getFloat("width") : 3f;
        this.height = nbt.contains("height") ? nbt.getFloat("height") : 3f;
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("picture", this.getStack().writeNbt(new NbtCompound()));
        nbt.putFloat("offsetX", this.offsetX);
        nbt.putFloat("offsetY", this.offsetY);
        nbt.putFloat("width", this.width);
        nbt.putFloat("height", this.height);

        super.writeNbt(nbt);
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = Inventories.removeStack(this.inventory, slot);
        this.updateUuid();
        this.sync();
        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.inventory.set(slot, stack);
        this.updateUuid();
        this.sync();
    }

    public void resize(float offsetX, float offsetY, float width, float height) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.width = width;
        this.height = height;

        this.sync();
    }

    private void sync() {
        this.markDirty();

        if (world != null) {
            world.updateListeners(pos, getCachedState(), getCachedState(), Block.NOTIFY_ALL);
        }
    }

    private void updateUuid() {
        ItemStack stack = getStack();
        if (stack.isOf(Camerapture.PICTURE)) {
            pictureUuid = PictureItem.getUuid(stack);
        } else {
            pictureUuid = null;
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    public UUID getPictureUuid() {
        return pictureUuid;
    }

    public float getOffsetX() {
        return offsetX;
    }

    public float getOffsetY() {
        return offsetY;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    @Override
    public int[] getAvailableSlots(Direction side) {
        return new int[]{0};
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return canInsert(stack);
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack, Direction dir) {
        return true;
    }

    public static boolean canInsert(ItemStack stack) {
        return stack.isOf(Camerapture.PICTURE);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.camerapture.display");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new DisplayScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(this.offsetX);
        buf.writeFloat(this.offsetY);
        buf.writeFloat(this.width);
        buf.writeFloat(this.height);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
