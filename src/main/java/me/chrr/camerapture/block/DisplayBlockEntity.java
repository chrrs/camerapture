package me.chrr.camerapture.block;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.screen.DisplayScreenHandler;
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
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DisplayBlockEntity extends BlockEntity implements NamedScreenHandlerFactory, SingleStackInventory, SidedInventory {
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);

    private UUID pictureUuid;

    public DisplayBlockEntity(BlockPos pos, BlockState state) {
        super(Camerapture.DISPLAY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        if (nbt.contains("picture", NbtElement.COMPOUND_TYPE)) {
            this.setStack(ItemStack.fromNbt(nbt.getCompound("picture")));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.put("picture", this.getStack().writeNbt(new NbtCompound()));
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
