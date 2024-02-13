package me.chrr.camerapture.block;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SingleStackInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class DisplayBlockEntity extends BlockEntity implements SingleStackInventory {
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
        super.writeNbt(nbt);

        if (!nbt.isEmpty()) {
            nbt.put("picture", this.getStack().writeNbt(new NbtCompound()));
        }
    }

    @Override
    public ItemStack getStack(int slot) {
        return this.inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack stack = Inventories.removeStack(this.inventory, slot);

        if (!stack.isEmpty()) {
            pictureUuid = null;
        }

        return stack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (canInsert(stack)) {
            this.inventory.set(slot, stack);
            pictureUuid = PictureItem.getUuid(stack);
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    public UUID getPictureUuid() {
        return pictureUuid;
    }

    public static boolean canInsert(ItemStack stack) {
        return stack.isOf(Camerapture.PICTURE);
    }
}
