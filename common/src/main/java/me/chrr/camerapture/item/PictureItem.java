package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


public class PictureItem extends Item {
    public static final Identifier ID = Camerapture.id("picture");
    public static final RegistryKey<Item> KEY = RegistryKey.of(RegistryKeys.ITEM, ID);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("MMM d, yyyy 'at' HH:mm");

    public PictureItem() {
        super(new Settings());
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!user.isSneaking()) {
            // When not sneaking, we show the picture client-side through an event handler.
            return TypedActionResult.success(stack);
        } else {
            return TypedActionResult.pass(stack);
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || !player.isSneaking()) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        Direction facing = context.getSide();
        BlockPos pos = context.getBlockPos().offset(facing);
        ItemStack itemStack = context.getStack();

        // Pictures can only be placed on walls.
        if (facing.getAxis().isVertical() || !player.canPlaceOn(pos, facing, itemStack)) {
            return ActionResult.PASS;
        }

        PictureFrameEntity pictureFrameEntity = new PictureFrameEntity(world, pos, facing);
        if (!pictureFrameEntity.canStayAttached()) {
            return ActionResult.PASS;
        }

        // Correctly handle (+NBT) items
        NbtCompound nbtCompound = itemStack.getNbt();
        if (nbtCompound != null) {
            EntityType.loadFromEntityNbt(world, player, pictureFrameEntity, nbtCompound);
        }

        pictureFrameEntity.setItemStack(itemStack.copyWithCount(1));

        if (!world.isClient) {
            pictureFrameEntity.onPlace();
            world.emitGameEvent(player, GameEvent.ENTITY_PLACE, pictureFrameEntity.getPos());
            world.spawnEntity(pictureFrameEntity);
        }

        itemStack.decrement(1);
        return ActionResult.SUCCESS;
    }

    public static ItemStack create(String creator, UUID uuid) {
        ItemStack stack = new ItemStack(Camerapture.PICTURE, 1);
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString("creator", creator);
        nbt.putUuid("uuid", uuid);
        nbt.putLong("timestamp", System.currentTimeMillis());
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        getTooltip(tooltip::add, stack);
    }

    public static void getTooltip(Consumer<Text> textConsumer, ItemStack stack) {
        PictureData data = getPictureData(stack);
        if (data == null) {
            return;
        }

        textConsumer.accept(Text.translatable(
                "item.camerapture.picture.creator_tooltip",
                Text.literal(data.creator).formatted(Formatting.GRAY)
        ).formatted(Formatting.DARK_GRAY));

        String timestamp = SDF.format(new Date(data.timestamp));
        textConsumer.accept(Text.translatable(
                "item.camerapture.picture.timestamp_tooltip",
                Text.literal(timestamp).formatted(Formatting.GRAY)
        ).formatted(Formatting.DARK_GRAY));
    }

    @Nullable
    public static PictureData getPictureData(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("uuid")) {
            return new PictureData(
                    nbt.getUuid("uuid"),
                    nbt.getString("creator"),
                    nbt.getLong("timestamp")
            );
        } else {
            return null;
        }
    }

    public record PictureData(UUID id, String creator, long timestamp) {
    }
}