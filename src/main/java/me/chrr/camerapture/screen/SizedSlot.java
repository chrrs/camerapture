package me.chrr.camerapture.screen;

import net.minecraft.inventory.Inventory;
import net.minecraft.screen.slot.Slot;

public class SizedSlot extends Slot {
    private final int width;
    private final int height;

    public SizedSlot(Inventory inventory, int index, int x, int y, int width, int height) {
        super(inventory, index, x, y);
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
