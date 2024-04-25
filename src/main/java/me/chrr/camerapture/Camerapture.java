package me.chrr.camerapture;

import me.chrr.camerapture.entity.PictureEntity;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.PartialPicturePacket;
import me.chrr.camerapture.net.PictureErrorPacket;
import me.chrr.camerapture.net.RequestPicturePacket;
import me.chrr.camerapture.net.TakePicturePacket;
import me.chrr.camerapture.picture.ServerPictureStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
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

    public static final Item CAMERA = new CameraItem(new FabricItemSettings().maxCount(1));
    public static final Item PICTURE = new PictureItem(new FabricItemSettings());

    public static final EntityType<PictureEntity> PICTURE_ENTITY =
            FabricEntityTypeBuilder.<PictureEntity>create(SpawnGroup.MISC, PictureEntity::new)
                    .dimensions(new EntityDimensions(0.5f, 0.5f, false))
                    .trackRangeChunks(10)
                    .build();

    public static final Identifier PICTURES_TAKEN = id("pictures_taken");

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("camera"), CAMERA);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> content.add(CAMERA));

        Registry.register(Registries.ITEM, id("picture"), PICTURE);

        Registry.register(Registries.ENTITY_TYPE, id("picture_frame"), PICTURE_ENTITY);

        Registry.register(Registries.CUSTOM_STAT, "pictures_taken", PICTURES_TAKEN);
        Stats.CUSTOM.getOrCreateStat(PICTURES_TAKEN, StatFormatter.DEFAULT);

        // Client right-clicked with a camera
        ServerPlayNetworking.registerGlobalReceiver(TakePicturePacket.TYPE, (packet, player, sender) -> {
            Pair<Hand, ItemStack> activeCamera = findActiveCamera(player);
            if (activeCamera == null) {
                return;
            }

            if (Inventories.remove(player.getInventory(), (stack) -> stack.isOf(Items.PAPER), 1, false) != 1) {
                return;
            }

            CameraItem.setActive(activeCamera.getRight(), false);
            player.getItemCooldownManager().set(activeCamera.getRight().getItem(), 20 * 3);
            player.swingHand(activeCamera.getLeft(), true);

            player.incrementStat(PICTURES_TAKEN);

            UUID uuid = ServerPictureStore.getInstance().reserveUuid();
            ServerPlayNetworking.send(player, new RequestPicturePacket(uuid));
        });

        // Client sends back a picture following a take-picture request
        Map<UUID, ByteCollector> collectors = new HashMap<>();
        ServerPlayNetworking.registerGlobalReceiver(PartialPicturePacket.TYPE, (packet, player, sender) -> {
            if (!ServerPictureStore.getInstance().isReserved(packet.uuid())) {
                LOGGER.error(player.getName().toString() + " tried to send a byte section for an unreserved UUID");
                return;
            }

            if (packet.bytesLeft() > Camerapture.MAX_IMAGE_BYTES) {
                LOGGER.error(player.getName().getString() + " sent a picture exceeding the size limit");
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveUuid(packet.uuid());
            }

            ByteCollector collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                collectors.remove(uuid);
                ThreadPooler.run(() -> {
                    try {
                        ServerPictureStore.getInstance().put(player.getServer(), uuid, new ServerPictureStore.Picture(bytes));
                        ItemStack picture = PictureItem.create(player.getName().getString(), uuid);
                        player.getInventory().offerOrDrop(picture);
                    } catch (Exception e) {
                        LOGGER.error("failed to save picture from " + player.getName().getString(), e);
                        player.sendMessage(Text.translatable("text.camerapture.picture_failed").formatted(Formatting.RED));
                    }
                });
            }));

            if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                LOGGER.error(player.getName().getString() + "sent a malformed byte section");
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveUuid(packet.uuid());
            }
        });

        // Client requests a picture with a certain UUID
        ServerPlayNetworking.registerGlobalReceiver(RequestPicturePacket.TYPE, (packet, player, sender) -> {
            try {
                ServerPictureStore.Picture picture = ServerPictureStore.getInstance().get(player.getServer(), packet.uuid());

                if (picture == null) {
                    LOGGER.warn(player.getName().getString() + " requested a picture with an unknown UUID");
                    ServerPlayNetworking.send(player, new PictureErrorPacket(packet.uuid()));
                } else {
                    ByteCollector.split(picture.bytes(), SECTION_SIZE, (section, bytesLeft) ->
                            ServerPlayNetworking.send(player, new PartialPicturePacket(packet.uuid(), section, bytesLeft)));
                }
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

    public static Identifier id(String path) {
        return new Identifier("camerapture", path);
    }
}
