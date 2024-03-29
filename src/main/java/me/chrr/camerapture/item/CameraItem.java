package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class CameraItem extends Item {
    public CameraItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        setActive(stack, !isActive(stack));

        user.incrementStat(Stats.USED.getOrCreateStat(Camerapture.CAMERA));

        return TypedActionResult.success(stack);
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateNbt().putBoolean("active", active);
    }

    public static boolean isActive(ItemStack stack) {
        NbtCompound nbt = stack.getOrCreateNbt();
        return nbt.getBoolean("active");
    }
}
