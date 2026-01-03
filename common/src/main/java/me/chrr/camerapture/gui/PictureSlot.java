package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class PictureSlot extends SizedSlot {
    private boolean enabled = false;

    public PictureSlot(Container inventory, int index, int x, int y) {
        super(inventory, index, x, y, 48, 27);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(Camerapture.PICTURE);
    }

    @Override
    public boolean isActive() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}