package me.chrr.camerapture;

import me.chrr.camerapture.block.DisplayBlock;
import me.chrr.camerapture.block.DisplayBlockEntity;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.PartialPicturePacket;
import me.chrr.camerapture.net.PictureErrorPacket;
import me.chrr.camerapture.net.RequestPicturePacket;
import me.chrr.camerapture.net.TakePicturePacket;
import me.chrr.camerapture.picture.ServerImageStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Camerapture implements ModInitializer {
    public static final int MAX_IMAGE_BYTES = 200_000;
    public static final int MAX_IMAGE_SIZE = 1280;
    public static final int SECTION_SIZE = 30_000;

    private static final Logger LOGGER = LogManager.getLogger();

    public static final Block DISPLAY = new DisplayBlock(FabricBlockSettings.create().nonOpaque());
    public static final BlockEntityType<DisplayBlockEntity> DISPLAY_BLOCK_ENTITY = FabricBlockEntityTypeBuilder.create(DisplayBlockEntity::new, DISPLAY).build();

    public static final Item CAMERA = new CameraItem(new FabricItemSettings().maxCount(1));
    public static final Item PICTURE = new PictureItem(new FabricItemSettings());

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, new Identifier("camerapture", "display"), DISPLAY);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier("camerapture", "display_block_entity"), DISPLAY_BLOCK_ENTITY);
        Registry.register(Registries.ITEM, new Identifier("camerapture", "display"), new BlockItem(DISPLAY, new FabricItemSettings()));

        Registry.register(Registries.ITEM, new Identifier("camerapture", "camera"), CAMERA);
        Registry.register(Registries.ITEM, new Identifier("camerapture", "picture"), PICTURE);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> content.add(CAMERA));

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

        Map<UUID, ByteCollector> collectors = new HashMap<>();
        ServerPlayNetworking.registerGlobalReceiver(PartialPicturePacket.TYPE, (packet, player, sender) -> {
            ByteCollector collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                collectors.remove(uuid);
                ThreadPooler.run(() -> {
                    try {
                        ServerImageStore.getInstance().put(player.getServer(), uuid, new ServerImageStore.Image(bytes));
                        ItemStack picture = PictureItem.create(player.getName().getString(), uuid);
                        player.getInventory().offerOrDrop(picture);
                    } catch (Exception e) {
                        LOGGER.error("failed to save picture from " + player.getName().getString(), e);
                        player.sendMessage(Text.translatable("text.camerapture.picture_failed").formatted(Formatting.RED));
                    }
                });
            }));

            if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                LOGGER.error("received malformed byte section from " + player.getName().getString());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestPicturePacket.TYPE, (packet, player, sender) -> {
            try {
                ServerImageStore.Image image = ServerImageStore.getInstance().get(player.getServer(), packet.uuid());

                ByteCollector.split(image.bytes(), SECTION_SIZE, (section, bytesLeft) ->
                        ServerPlayNetworking.send(player, new PartialPicturePacket(packet.uuid(), section, bytesLeft)));
            } catch (Exception e) {
                LOGGER.error("failed to load picture for " + player.getName().getString(), e);
                ServerPlayNetworking.send(player, new PictureErrorPacket(packet.uuid()));
            }
        });
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
