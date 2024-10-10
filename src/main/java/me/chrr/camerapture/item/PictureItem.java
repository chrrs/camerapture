package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

//? if >=1.20.5 {
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
//?} else
/*import net.minecraft.nbt.NbtCompound;*/

//? if >=1.21 {
import net.minecraft.item.tooltip.TooltipData;
        //?} else
/*import net.minecraft.client.item.TooltipData;*/

public class PictureItem extends Item {
    private static final SimpleDateFormat SDF = new SimpleDateFormat("MMM d, yyyy 'at' HH:mm");

    public PictureItem(Settings settings) {
        super(settings);
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
        //? if >=1.20.5 {
        NbtComponent nbtComponent = itemStack.getOrDefault(DataComponentTypes.ENTITY_DATA, NbtComponent.DEFAULT);
        if (!nbtComponent.isEmpty()) {
            EntityType.loadFromEntityNbt(world, player, pictureFrameEntity, nbtComponent);
        }
        //?} else {
        /*NbtCompound nbtCompound = itemStack.getNbt();
        if (nbtCompound != null) {
            EntityType.loadFromEntityNbt(world, player, pictureFrameEntity, nbtCompound);
        }
        *///?}

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

        //? if >=1.20.5 {
        stack.set(Camerapture.PICTURE_DATA, new PictureData(uuid, creator, System.currentTimeMillis()));
        //?} else {
        /*NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString("creator", creator);
        nbt.putUuid("uuid", uuid);
        nbt.putLong("timestamp", System.currentTimeMillis());
        *///?}

        return stack;
    }

    //? if >=1.20.5 {
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
        getTooltip(tooltip::add, stack);
    }
    //?} else {
    /*@Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, net.minecraft.client.item.TooltipContext context) {
        getTooltip(tooltip::add, stack);
    }
    *///?}

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
        //? if >=1.20.5 {
        return stack.get(Camerapture.PICTURE_DATA);
        //?} else {
        /*NbtCompound nbt = stack.getOrCreateNbt();
        if (nbt.contains("uuid")) {
            return new PictureData(
                    nbt.getUuid("uuid"),
                    nbt.getString("creator"),
                    nbt.getLong("timestamp")
            );
        } else {
            return null;
        }
        *///?}
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        return Optional.ofNullable(PictureItem.getPictureData(stack)).map(data -> new PictureTooltipData(data.id()));
    }

    public record PictureData(UUID id, String creator, long timestamp) {
        //? if >=1.20.5 {
        public static Codec<PictureData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Uuids.CODEC.fieldOf("id").forGetter(component -> component.id),
                Codec.STRING.fieldOf("creator").forGetter(component -> component.creator),
                Codec.LONG.fieldOf("timestamp").forGetter(component -> component.timestamp)
        ).apply(instance, PictureData::new));

        public static PacketCodec<ByteBuf, PictureData> PACKET_CODEC = PacketCodecs.codec(CODEC);
        //?}
    }
}
