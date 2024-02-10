package me.chrr.camerapture;

import me.chrr.camerapture.block.PictureFrameBlock;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.PictureErrorPacket;
import me.chrr.camerapture.net.PicturePacket;
import me.chrr.camerapture.net.RequestPicturePacket;
import me.chrr.camerapture.net.TakePicturePacket;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.ClientPictureTaker;
import me.chrr.camerapture.picture.ServerImageStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class Camerapture implements ModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final Block PICTURE_FRAME = new PictureFrameBlock(FabricBlockSettings.copyOf(Blocks.OAK_PLANKS));
    public static final Item CAMERA = new CameraItem(new FabricItemSettings().maxCount(1));
    public static final Item PICTURE = new PictureItem(new FabricItemSettings());

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, new Identifier("camerapture", "picture_frame"), PICTURE_FRAME);
        Registry.register(Registries.ITEM, new Identifier("camerapture", "picture_frame"), new BlockItem(PICTURE_FRAME, new FabricItemSettings()));
        Registry.register(Registries.ITEM, new Identifier("camerapture", "camera"), CAMERA);
        Registry.register(Registries.ITEM, new Identifier("camerapture", "picture"), PICTURE);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> content.add(CAMERA));

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (isCameraActive(player)) {
                ClientPlayNetworking.send(new TakePicturePacket());
                return true;
            } else {
                return false;
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(RequestPicturePacket.TYPE, ((packet, player, sender) ->
                ClientPictureTaker.getInstance().takePicture(packet.uuid())));

        ServerPlayNetworking.registerGlobalReceiver(TakePicturePacket.TYPE, (packet, player, sender) -> {
            Pair<Hand, ItemStack> activeCamera = findActiveCamera(player);
            if (activeCamera == null) {
                return;
            }

            CameraItem.setActive(activeCamera.getRight(), false);
            player.getItemCooldownManager().set(activeCamera.getRight().getItem(), 20 * 3);
            player.swingHand(activeCamera.getLeft(), true);

            UUID uuid = ServerImageStore.getInstance().reserveUuid();
            ServerPlayNetworking.send(player, new RequestPicturePacket(uuid));
        });

        ServerPlayNetworking.registerGlobalReceiver(PicturePacket.TYPE, (packet, player, sender) ->
                ThreadManager.getInstance().run(() -> {
                    try {
                        ServerImageStore.getInstance().put(player.getServer(), packet.uuid(), new ServerImageStore.Image(packet.bytes()));
                        ItemStack picture = PictureItem.create(player.getName().getString(), packet.uuid());
                        player.getInventory().offerOrDrop(picture);
                    } catch (Exception e) {
                        LOGGER.error("failed to save picture from " + player.getName().getString(), e);
                        player.sendMessage(Text.translatable("text.camerapture.picture_failed").formatted(Formatting.RED));
                    }
                }));

        ServerPlayNetworking.registerGlobalReceiver(RequestPicturePacket.TYPE, (packet, player, sender) ->
                ThreadManager.getInstance().run(() -> {
                    try {
                        ServerImageStore.Image image = ServerImageStore.getInstance().get(player.getServer(), packet.uuid());
                        ServerPlayNetworking.send(player, new PicturePacket(packet.uuid(), image.bytes()));
                    } catch (Exception e) {
                        LOGGER.error("failed to load picture for " + player.getName().getString(), e);
                        ServerPlayNetworking.send(player, new PictureErrorPacket(packet.uuid()));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(PicturePacket.TYPE, (packet, player, sender) ->
                ThreadManager.getInstance().run(() ->
                        ClientPictureStore.getInstance().processReceivedBytes(packet.uuid(), packet.bytes())));

        ClientPlayNetworking.registerGlobalReceiver(PictureErrorPacket.TYPE, (packet, player, sender) ->
                ClientPictureStore.getInstance().processReceivedError(packet.uuid()));
    }

    @Nullable
    public static Pair<Hand, ItemStack> findActiveCamera(PlayerEntity player) {
        if (player == null) {
            return null;
        }

        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(Camerapture.CAMERA) && CameraItem.isActive(stack)) {
                return new Pair<>(hand, stack);
            }
        }

        return null;
    }

    public static boolean isCameraActive(PlayerEntity player) {
        return findActiveCamera(player) != null;
    }
}
