package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.block.DisplayBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;

public class DisplayScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    public BlockPos pos = BlockPos.ORIGIN;

    public float initialOffsetX = 0f;
    public float initialOffsetY = 0f;
    public float initialWidth = 3f;
    public float initialHeight = 3f;

    public DisplayScreenHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf buf) {
        this(syncId, playerInventory, new SimpleInventory(1));

        this.pos = buf.readBlockPos();

        this.initialOffsetX = buf.readFloat();
        this.initialOffsetY = buf.readFloat();
        this.initialWidth = buf.readFloat();
        this.initialHeight = buf.readFloat();
    }

    public DisplayScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(Camerapture.DISPLAY_SCREEN_HANDLER, syncId);
        checkSize(inventory, 1);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        this.addSlot(new Slot(inventory, 0, 8, 8) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return DisplayBlockEntity.canInsert(stack);
            }
        });

        for (int x = 0; x < 9; x++) {
            for (int y = 0; y < 3; y++) {
                this.addSlot(new Slot(playerInventory, 9 + x + y * 9, 8 + x * 18, 94 + y * 18));
            }
        }

        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(playerInventory, x, 8 + x * 18, 152));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotId) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotId);
        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (slotId == 0) {
                if (!this.insertItem(originalStack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (DisplayBlockEntity.canInsert(originalStack)) {
                if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }
}
