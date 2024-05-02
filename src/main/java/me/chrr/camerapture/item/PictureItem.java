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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PictureItem extends Item {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MMM d, yyyy 'at' HH:mm");

    public PictureItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isSneaking()) {
            // It shows client side.
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
        return ActionResult.success(world.isClient);
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
        NbtCompound nbt = stack.getOrCreateNbt();

        if (nbt.contains("creator")) {
            tooltip.add(Text.translatable(
                    "item.camerapture.picture.creator_tooltip",
                    Text.literal(nbt.getString("creator")).formatted(Formatting.GRAY)
            ).formatted(Formatting.DARK_GRAY));
        }

        if (nbt.contains("timestamp")) {
            String timestamp = SDF.format(new Date(nbt.getLong("timestamp")));
            tooltip.add(Text.translatable(
                    "item.camerapture.picture.timestamp_tooltip",
                    Text.literal(timestamp).formatted(Formatting.GRAY)
            ).formatted(Formatting.DARK_GRAY));
        }
    }

    @Nullable
    public static UUID getUuid(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("uuid")) {
            return nbt.getUuid("uuid");
        } else {
            return null;
        }
    }
}
