package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.ShowPicturePacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
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
        NbtCompound nbt = stack.getOrCreateNbt();

        if (nbt.contains("uuid")) {
            if (!world.isClient) {
                ServerPlayNetworking.send((ServerPlayerEntity) user, new ShowPicturePacket(nbt.getUuid("uuid")));
            }

            user.incrementStat(Stats.USED.getOrCreateStat(Camerapture.PICTURE));

            return TypedActionResult.success(stack);
        } else {
            return TypedActionResult.pass(stack);
        }
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
