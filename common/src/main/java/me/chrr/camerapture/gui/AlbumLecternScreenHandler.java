package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class AlbumLecternScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public AlbumLecternScreenHandler(int syncId) {
        this(syncId, new SimpleInventory(1));
    }

    public AlbumLecternScreenHandler(int syncId, Inventory inventory) {
        super(Camerapture.ALBUM_LECTERN_SCREEN_HANDLER, syncId);

        checkSize(inventory, 1);
        this.inventory = inventory;

        this.addSlot(new Slot(inventory, 0, 0, 0) {
            @Override
            public void markDirty() {
                super.markDirty();
                AlbumLecternScreenHandler.this.onContentChanged(inventory);
            }
        });
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!player.canModifyBlocks()) {
            return false;
        }

        // The only button click that is sent over to the server
        // is 'Take Book'.
        ItemStack itemStack = this.inventory.removeStack(0);
        this.inventory.markDirty();
        if (!player.getInventory().insertStack(itemStack)) {
            player.dropItem(itemStack, false);
        }

        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}