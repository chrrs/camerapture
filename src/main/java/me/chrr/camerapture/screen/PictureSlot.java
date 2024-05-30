package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

public class PictureSlot extends Slot {
    public PictureSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.isOf(Camerapture.PICTURE);
    }
}
