package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.screen.AlbumScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

//? if <1.20.4
/*import net.minecraft.util.collection.DefaultedList;*/

//? if >=1.20.5 {
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
//?} else {
/*import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LecternBlock;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
*///?}

public class AlbumItem extends Item {
    public static int PAGES = 3;
    public static int ITEMS_PER_PAGE = 12;
    public static int SLOTS = PAGES * ITEMS_PER_PAGE;

    public AlbumItem(Settings settings) {
        super(settings);
    }

    // In newer versions, you can place any items with the `lectern_books` tag in lecterns automatically.
    //? if <1.20.5 {
    /*@Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.isOf(Blocks.LECTERN)) {
            return LecternBlock.putBookIfAbsent(context.getPlayer(), world, pos, state, context.getStack())
                    ? ActionResult.success(world.isClient)
                    : ActionResult.PASS;
        }

        return ActionResult.PASS;
    }
    *///?}

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient && player.isSneaking()) {
            // We write the slot with the album in it. If the album is in
            // the offhand, we can ignore it.
            int albumSlot = hand == Hand.MAIN_HAND ? player.getInventory().selectedSlot : -1;

            //noinspection rawtypes
            player.openHandledScreen(new ExtendedScreenHandlerFactory() {
                //? if >=1.20.5 {
                @Override
                public Integer getScreenOpeningData(ServerPlayerEntity player) {
                    return albumSlot;
                }
                //?} else {
                /*@Override
                public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                    buf.writeInt(albumSlot);
                }
                *///?}

                @Override
                public Text getDisplayName() {
                    return stack.getName();
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new AlbumScreenHandler(syncId, playerInventory, new AlbumInventory(hand, stack), albumSlot);
                }
            });
        }

        return TypedActionResult.success(stack);
    }

    //? if >=1.20.5 {
    public static List<ItemStack> getPictures(ItemStack album) {
        ContainerComponent container = album.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            return container.streamNonEmpty().toList();
        } else {
            return List.of();
        }
    }
    //?} else {
    /*public static List<ItemStack> getPictures(ItemStack album) {
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
    *///?}

    //? if >=1.20.5 {
    @Override
    public boolean allowComponentsUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    //?} else {
    /*@Override
    public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
        return false;
    }
    *///?}


    public static class AlbumInventory extends SimpleInventory {
        private final Hand hand;

        private AlbumInventory(Hand hand, ItemStack stack) {
            super(SLOTS);
            this.hand = hand;

            //? if >=1.20.5 {
            ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
            if (container != null) {
                container.copyTo(this.getHeldStacks());
            }
            //?} else {
            /*NbtCompound nbt = stack.getNbt();
            if (nbt != null) {
                Inventories.readNbt(nbt, this.getHeldStacks());
            }
            *///?}
        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            // FIXME: Duplication? Swapping the album with another one doesn't change
            //        this condition I think.
            return getAlbumStack(player).isOf(Camerapture.ALBUM);
        }

        @Override
        public void onClose(PlayerEntity player) {
            //? if >=1.20.5 {
            this.getAlbumStack(player).set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(this.getHeldStacks()));
            //?} else
            /*Inventories.writeNbt(getAlbumStack(player).getOrCreateNbt(), getHeldStacks());*/
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
        //? if <1.20.4 {
        /*private DefaultedList<ItemStack> getHeldStacks() {
            return this.stacks;
        }
        *///?}
    }
}
