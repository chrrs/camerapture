package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.screen.AlbumScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class AlbumItem extends Item {
    public static int PAGES = 3;

    public AlbumItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient && player.isSneaking()) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, _player) ->
                            new AlbumScreenHandler(syncId, playerInventory, getInventory(stack)),
                    stack.getName()));
        }

        return TypedActionResult.success(stack);
    }

    public static Inventory getInventory(ItemStack stack) {
        return new AlbumInventory(stack);
    }

    public static List<ItemStack> getPictures(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains("Items")) {
            return List.of();
        }

        NbtList items = nbt.getList("Items", NbtElement.COMPOUND_TYPE);
        List<ItemStack> pictures = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack itemStack = ItemStack.fromNbt(items.getCompound(i));
            if (!itemStack.isEmpty() && itemStack.isOf(Camerapture.PICTURE)) {
                pictures.add(itemStack);
            }
        }

        return pictures;
    }

    public static class AlbumInventory extends SimpleInventory {
        private final ItemStack stack;

        private AlbumInventory(ItemStack stack) {
            super(PAGES * 12);

            this.stack = stack;

            NbtCompound nbt = stack.getOrCreateNbt();
            if (nbt.contains("Items")) {
                readNbtList(nbt.getList("Items", NbtElement.COMPOUND_TYPE));
            }
        }

        @Override
        public void markDirty() {
            stack.getOrCreateNbt().put("Items", toNbtList());
            super.markDirty();
        }

        @Override
        public int getMaxCountPerStack() {
            return 1;
        }
    }
}
