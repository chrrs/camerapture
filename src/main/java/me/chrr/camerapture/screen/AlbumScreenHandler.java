package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.AlbumItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class AlbumScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public AlbumScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(AlbumItem.PAGES * 12));
    }

    public AlbumScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(Camerapture.ALBUM_SCREEN_HANDLER, syncId);

        checkSize(inventory, AlbumItem.PAGES * 4);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        // Album inventory
        for (int side = 0; side < 2; side++) {
            for (int x = 0; x < 6; x++) {
                for (int y = 0; y < 3; y++) {
                    int dx = side * 131 + 22 + x * 18;
                    int dy = 27 + y * 18;
                    this.addSlot(new PictureSlot(inventory, side * 18 + y * 6 + x, dx, dy));
                }
            }
        }

        // Player inventory
        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 3; y++) {
                this.addSlot(new Slot(playerInventory, 9 + y * 9 + x, 60 + x * 18, 155 + y * 18));
            }
        }

        // Player hotbar
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(playerInventory, x, 60 + x * 18, 213));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }
}
