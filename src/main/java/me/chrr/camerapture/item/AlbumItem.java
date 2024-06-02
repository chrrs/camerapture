package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.screen.AlbumScreenHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class AlbumItem extends Item {
    public static int PAGES = 3;
    public static int ITEMS_PER_PAGE = 12;
    public static int SLOTS = PAGES * ITEMS_PER_PAGE;

    public AlbumItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient && player.isSneaking()) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, playerInventory, _player) ->
                            new AlbumScreenHandler(syncId, playerInventory, new AlbumInventory(hand, stack)),
                    stack.getName()));
        }

        return TypedActionResult.success(stack);
    }

    public static List<ItemStack> getPictures(ItemStack album) {
        NbtCompound nbt = album.getNbt();
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

    @Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }

    public static class AlbumInventory extends SimpleInventory {
        private final Hand hand;

        private AlbumInventory(Hand hand, ItemStack stack) {
            super(SLOTS);
            this.hand = hand;

            NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                Inventories.readNbt(nbt, this.getHeldStacks());
            }
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            // FIXME: Duplication? Swapping the album with another one doesn't change
            //        this condition I think.
            return getAlbumStack(player).isOf(Camerapture.ALBUM);
        }

        @Override
        public void onClose(PlayerEntity player) {
            Inventories.writeNbt(getAlbumStack(player).getOrCreateNbt(), getHeldStacks());
            super.onClose(player);
        }

        @Override
        public int getMaxCountPerStack() {
            return 1;
        }

        private ItemStack getAlbumStack(PlayerEntity player) {
            return player.getStackInHand(this.hand);
        }

        // This method exists on 1.20.4, but not on 1.20.1
        /*? if <1.20.4 {*//*
        private DefaultedList<ItemStack> getHeldStacks() {
            return this.stacks;
        }
        *//*?}*/
    }
}
