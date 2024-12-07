package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;

public class PictureSlot extends SizedSlot {
    private boolean visible = false;

    public PictureSlot(Inventory inventory, int index, int x, int y) {
        super(inventory, index, x, y, 48, 27);
    }

    @Override
    public boolean canInsert(ItemStack stack) {
        return stack.isOf(Camerapture.PICTURE);
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}