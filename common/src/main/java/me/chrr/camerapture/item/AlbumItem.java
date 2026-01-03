package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.gui.AlbumMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import java.util.List;

public class AlbumItem extends Item {
    public static final Identifier ID = Camerapture.id("album");
    public static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, ID);

    public static int PAGES = 3;
    public static int ITEMS_PER_PAGE = 12;
    public static int SLOTS = PAGES * ITEMS_PER_PAGE;

    public AlbumItem() {
        super(new Item.Properties().setId(KEY).stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // On the server side, we open the album inventory UI.
        // Viewing the pictures is handled on the client side, see CameraptureClient#onUseItem.
        if (!level.isClientSide()) {
            if (player.isShiftKeyDown() || AlbumItem.getPictures(stack).isEmpty()) {
                player.openMenu(new SimpleMenuProvider((containerId, playerInventory, playerEntity) ->
                        new AlbumMenu(containerId, playerInventory, new AlbumInventory(hand, stack)), stack.getHoverName()));
            }
        }

        return InteractionResult.SUCCESS;
    }

    public static List<ItemStack> getPictures(ItemStack album) {
        ItemContainerContents container = album.get(DataComponents.CONTAINER);
        if (container != null) {
            return container.nonEmptyStream().toList();
        } else {
            return List.of();
        }
    }

    public static class AlbumInventory extends SimpleContainer {
        private final InteractionHand hand;

        private AlbumInventory(InteractionHand hand, ItemStack stack) {
            super(SLOTS);
            this.hand = hand;

            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container != null) {
                container.copyInto(this.getItems());
            }
        }

        @Override
        public boolean stillValid(Player player) {
            // FIXME: Duplication? Swapping the album with another one doesn't change
            //        this condition I think.
            return getAlbumStack(player).is(Camerapture.ALBUM);
        }

        @Override
        public void stopOpen(ContainerUser user) {
            if (!(user instanceof Player player)) {
                super.stopOpen(user);
                return;
            }

            ItemStack stack = this.getAlbumStack(player);
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
            stack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT.withHidden(DataComponents.CONTAINER, true));

            super.stopOpen(player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        private ItemStack getAlbumStack(Player player) {
            return player.getItemInHand(this.hand);
        }
    }
}