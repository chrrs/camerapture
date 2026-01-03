package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.config.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class CameraItem extends Item {
    public static final Identifier ID = Camerapture.id("camera");
    public static final ResourceKey<Item> KEY = ResourceKey.create(Registries.ITEM, ID);

    public CameraItem() {
        super(new Properties().setId(KEY).stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean active = isActive(stack);

        // Retrieve permissions from the config.
        Config.Server config = Camerapture.CONFIG_MANAGER.getConfig().server;

        // Note that when we sneak-right-click when the camera is not active,
        // the upload GUI is opened on the client side.
        if (active || (!player.isShiftKeyDown() && config.permissionLevels.canTakePicture(player))) {
            setActive(stack, !active);
            return InteractionResult.CONSUME;
        }

        // If we try to upload when it's disabled, we send a message to the player.
        if (player.isShiftKeyDown()) {
            if (!config.permissionLevels.canUpload(player)) {
                player.displayClientMessage(Component.translatable("text.camerapture.uploading_disabled").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // Deactivate the camera when it's not selected anymore.
    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (slot == null || !(entity instanceof Player player)) {
            setActive(stack, false);
            return;
        }

        if (player.getItemBySlot(slot) != stack) {
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
    public static int getPaperInInventory(Player player) {
        return player.getInventory().countItem(Items.PAPER);
    }

    /// Return if the player can take a picture. They can if they are
    /// either in creative mode, or have at least a single piece of paper.
    public static boolean canTakePicture(Player player) {
        return player.hasInfiniteMaterials() || getPaperInInventory(player) > 0;
    }

    /// Find the camera item that the player is holding, if any.
    @Nullable
    public static HeldCamera find(Player player, boolean shouldBeActive) {
        if (player == null) {
            return null;
        }

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack == null || !stack.is(Camerapture.CAMERA)) {
                continue;
            }

            if (!shouldBeActive || isActive(stack)) {
                return new HeldCamera(stack, hand);
            }
        }

        return null;
    }

    public record HeldCamera(ItemStack stack, InteractionHand hand) {
    }
}
