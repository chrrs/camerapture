package me.chrr.camerapture.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.block.PictureFrameBlock;
import me.chrr.camerapture.block.PictureFrameBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;


public class PictureItem extends Item {
    public static final Identifier ID = Camerapture.id("picture");
    public static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, ID);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("MMM d, yyyy 'at' HH:mm");

    public PictureItem() {
        super(new Properties().setId(KEY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            // When not sneaking, we show the picture client-side through an event handler.
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        Direction facing = context.getClickedFace();
        BlockPos pos = context.getClickedPos().relative(facing);
        ItemStack itemStack = context.getItemInHand();

        // Pictures can only be placed on walls.
        if (facing.getAxis().isVertical() || !player.mayUseItemAt(pos, facing, itemStack)) {
            return InteractionResult.PASS;
        }

        // Check that placement position is available
        if (!level.getBlockState(pos).canBeReplaced()) {
            return InteractionResult.PASS;
        }

        // Check backing block is solid
        BlockState backingState = level.getBlockState(context.getClickedPos());
        //noinspection deprecation
        if (!backingState.isSolid()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockState frameState = Camerapture.PICTURE_FRAME_BLOCK.defaultBlockState()
                    .setValue(PictureFrameBlock.FACING, facing);
            level.setBlock(pos, frameState, 3);

            if (level.getBlockEntity(pos) instanceof PictureFrameBlockEntity blockEntity) {
                blockEntity.setItemStack(itemStack.copyWithCount(1));
            }

            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ITEM_FRAME_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 1f, 1f);
            level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        }

        itemStack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    public static ItemStack create(String creator, UUID uuid) {
        ItemStack stack = new ItemStack(Camerapture.PICTURE, 1);
        stack.set(Camerapture.PICTURE_DATA, new PictureData(uuid, creator, System.currentTimeMillis()));
        return stack;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        getTooltip(textConsumer, stack);
    }

    public static void getTooltip(Consumer<Component> textConsumer, ItemStack stack) {
        PictureData data = getPictureData(stack);
        if (data == null) {
            return;
        }

        textConsumer.accept(Component.translatable(
                "item.camerapture.picture.creator_tooltip",
                Component.literal(data.creator).withStyle(ChatFormatting.GRAY)
        ).withStyle(ChatFormatting.DARK_GRAY));

        String timestamp = SDF.format(new Date(data.timestamp));
        textConsumer.accept(Component.translatable(
                "item.camerapture.picture.timestamp_tooltip",
                Component.literal(timestamp).withStyle(ChatFormatting.GRAY)
        ).withStyle(ChatFormatting.DARK_GRAY));
    }

    @Nullable
    public static PictureData getPictureData(ItemStack stack) {
        return stack.get(Camerapture.PICTURE_DATA);
    }

    public record PictureData(UUID id, String creator, long timestamp) {
        public static Codec<PictureData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.AUTHLIB_CODEC.fieldOf("id").forGetter(component -> component.id),
                Codec.STRING.fieldOf("creator").forGetter(component -> component.creator),
                Codec.LONG.fieldOf("timestamp").forGetter(component -> component.timestamp)
        ).apply(instance, PictureData::new));

        public static StreamCodec<ByteBuf, PictureData> PACKET_CODEC = ByteBufCodecs.fromCodec(CODEC);
    }
}