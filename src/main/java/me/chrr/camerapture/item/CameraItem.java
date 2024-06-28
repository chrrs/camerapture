package me.chrr.camerapture.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

//? if >=1.20.5
import me.chrr.camerapture.Camerapture;

public class CameraItem extends Item {
    public CameraItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!user.isSneaking()) {
            setActive(stack, !isActive(stack));
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!selected) {
            setActive(stack, false);
        }
    }

    //? if >=1.20.5 {
    public static void setActive(ItemStack stack, boolean active) {
        stack.set(Camerapture.CAMERA_ACTIVE, active);
    }

    public static boolean isActive(ItemStack stack) {
        return stack.get(Camerapture.CAMERA_ACTIVE) == Boolean.TRUE;
    }
    //?} else {
    /*public static void setActive(ItemStack stack, boolean active) {
        stack.getOrCreateNbt().putBoolean("active", active);
    }

    public static boolean isActive(ItemStack stack) {
        return stack.getOrCreateNbt().getBoolean("active");
    }
    *///?}
}
