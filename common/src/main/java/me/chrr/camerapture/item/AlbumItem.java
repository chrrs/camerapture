package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.gui.AlbumScreenHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class AlbumItem extends Item {
    public static final Identifier ID = Camerapture.id("album");
    public static final RegistryKey<Item> KEY = RegistryKey.of(RegistryKeys.ITEM, ID);

    public static int PAGES = 3;
    public static int ITEMS_PER_PAGE = 12;
    public static int SLOTS = PAGES * ITEMS_PER_PAGE;

    public AlbumItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // On the server side, we open the album inventory UI.
        // Viewing the pictures is handled on the client side, see CameraptureClient#onUseItem.
        if (!world.isClient) {
            if (player.isSneaking() || AlbumItem.getPictures(stack).isEmpty()) {
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity) ->
                        new AlbumScreenHandler(syncId, playerInventory, new AlbumInventory(hand, stack)), stack.getName()));
            }
        }

        return TypedActionResult.success(stack);
    }

    public static List<ItemStack> getPictures(ItemStack album) {
        ContainerComponent container = album.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            return container.streamNonEmpty().toList();
        } else {
            return List.of();
        }
    }

    public static class AlbumInventory extends SimpleInventory {
        private final Hand hand;

        private AlbumInventory(Hand hand, ItemStack stack) {
            super(SLOTS);
            this.hand = hand;

            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container != null) {
                container.copyTo(this.getHeldStacks());
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
            this.getAlbumStack(player).set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(this.getHeldStacks()));
            super.onClose(player);
        }

        @Override
        public int getMaxCountPerStack() {
            return 1;
        }

        private ItemStack getAlbumStack(PlayerEntity player) {
            return player.getStackInHand(this.hand);
        }
    }
}