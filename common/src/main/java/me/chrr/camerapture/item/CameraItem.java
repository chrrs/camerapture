package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CameraItem extends Item {
    public static final Identifier ID = Camerapture.id("camera");
    public static final RegistryKey<Item> KEY = RegistryKey.of(RegistryKeys.ITEM, ID);

    public CameraItem() {
        super(new Settings().maxCount(1));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        boolean active = isActive(stack);

        // Note that when we sneak-right-click when the camera is not active,
        // the upload GUI is opened on the client side.
        if (active || !player.isSneaking()) {
            setActive(stack, !active);
            return TypedActionResult.consume(stack);
        }

        return TypedActionResult.success(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (!selected) {
            setActive(stack, false);
        }
    }

    public static void setActive(ItemStack stack, boolean active) {
        stack.set(Camerapture.CAMERA_ACTIVE, active);
    }

    public static boolean isActive(ItemStack stack) {
        return stack.get(Camerapture.CAMERA_ACTIVE) == Boolean.TRUE;
    }

    /// Find the amount of paper that the player has.
    public static int getPaperInInventory(PlayerEntity player) {
        return player.getInventory().count(Items.PAPER);
    }

    /// Return if the player can take a picture. They can if they are
    /// either in creative mode, or have at least a single piece of paper.
    public static boolean canTakePicture(PlayerEntity player) {
        return player.isInCreativeMode() || getPaperInInventory(player) > 0;
    }

    /// Find the camera item that the player is holding, if any.
    @Nullable
    public static HeldCamera find(PlayerEntity player, boolean shouldBeActive) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (!shouldBeActive || isActive(stack)) {
                return new HeldCamera(stack, hand);
            }
        }

        return null;
    }

    public record HeldCamera(ItemStack stack, Hand hand) {
    }
}
