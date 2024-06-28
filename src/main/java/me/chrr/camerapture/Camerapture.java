package me.chrr.camerapture;

import me.chrr.camerapture.config.Config;
import me.chrr.camerapture.config.ConfigManager;
import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureCloningRecipe;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.*;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.serverbound.SyncConfigPacket;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.RequestPicturePacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import me.chrr.camerapture.picture.ServerPictureStore;
import me.chrr.camerapture.screen.AlbumScreenHandler;
import me.chrr.camerapture.screen.PictureFrameScreenHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//? if >=1.20.5
/*import net.minecraft.component.ComponentType;*/

public class Camerapture implements ModInitializer {
    public static final int SECTION_SIZE = 30_000;

    public static final Logger LOGGER = LogManager.getLogger();
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    // Camera
    public static final Item CAMERA = new CameraItem(new Item.Settings().maxCount(1));
    public static final SoundEvent CAMERA_SHUTTER = SoundEvent.of(id("camera_shutter"));
    public static final Identifier PICTURES_TAKEN = id("pictures_taken");

    // Picture
    public static final Item PICTURE = new PictureItem(new Item.Settings());
    public static final SpecialRecipeSerializer<PictureCloningRecipe> PICTURE_CLONING =
            new SpecialRecipeSerializer<>(PictureCloningRecipe::new);

    // Album
    public static final Item ALBUM = new AlbumItem(new Item.Settings().maxCount(1));
    public static final ScreenHandlerType<AlbumScreenHandler> ALBUM_SCREEN_HANDLER =
            new ExtendedScreenHandlerType<>(AlbumScreenHandler::new);

    // Picture Frame
    public static final EntityType<PictureFrameEntity> PICTURE_FRAME =
            EntityType.Builder.<PictureFrameEntity>create(PictureFrameEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.5f, 0.5f)
                    .maxTrackingRange(10)
                    .build("picture_frame");
    public static final ScreenHandlerType<PictureFrameScreenHandler> PICTURE_FRAME_SCREEN_HANDLER =
            new ScreenHandlerType<>((syncId, pi) -> new PictureFrameScreenHandler(syncId), FeatureSet.empty());

    // Data Components
    //? if >=1.20.5 {
    /*public static final ComponentType<PictureItem.PictureData> PICTURE_DATA = ComponentType.<PictureItem.PictureData>builder()
            .codec(PictureItem.PictureData.CODEC).packetCodec(PictureItem.PictureData.PACKET_CODEC)
            .build();
    *///?}

    private final Queue<QueuedPicture> pictureQueue = new LinkedList<>();

    @Override
    public void onInitialize() {
        try {
            CONFIG_MANAGER.load();
        } catch (IOException e) {
            LOGGER.error("failed to load config", e);
        }

        registerContent();
        registerPackets();
        registerEvents();
    }

    private void registerContent() {
        // Camera
        Registry.register(Registries.ITEM, id("camera"), CAMERA);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> content.add(CAMERA));

        Registry.register(Registries.SOUND_EVENT, CAMERA_SHUTTER.getId(), CAMERA_SHUTTER);

        Registry.register(Registries.CUSTOM_STAT, "pictures_taken", PICTURES_TAKEN);
        Stats.CUSTOM.getOrCreateStat(PICTURES_TAKEN, StatFormatter.DEFAULT);

        // Picture
        Registry.register(Registries.ITEM, id("picture"), PICTURE);
        Registry.register(Registries.RECIPE_SERIALIZER, id("picture_cloning"), PICTURE_CLONING);

        // Album
        Registry.register(Registries.ITEM, id("album"), ALBUM);
        Registry.register(Registries.SCREEN_HANDLER, id("album"), ALBUM_SCREEN_HANDLER);

        // Picture Frame
        Registry.register(Registries.ENTITY_TYPE, id("picture_frame"), PICTURE_FRAME);
        Registry.register(Registries.SCREEN_HANDLER, id("picture_frame"), PICTURE_FRAME_SCREEN_HANDLER);
    }

    private void registerPackets() {
        Networking.registerServerBound(NewPicturePacket.class, NewPicturePacket.NET_CODEC);
        Networking.registerServerBound(RequestPicturePacket.class, RequestPicturePacket.NET_CODEC);
        Networking.registerServerBound(UploadPartialPicturePacket.class, UploadPartialPicturePacket.NET_CODEC);
        Networking.registerClientBound(PictureErrorPacket.class, PictureErrorPacket.NET_CODEC);
        Networking.registerClientBound(RequestUploadPacket.class, RequestUploadPacket.NET_CODEC);
        Networking.registerClientBound(SyncConfigPacket.class, SyncConfigPacket.NET_CODEC);
        Networking.registerClientBound(DownloadPartialPicturePacket.class, DownloadPartialPicturePacket.NET_CODEC);

        // Client requests to take / upload a picture
        Networking.onServerPacketReceive(NewPicturePacket.class, (packet, player) -> {
            Pair<Hand, ItemStack> camera = findCamera(player, false);
            if (camera == null) {
                return;
            }

            if (Inventories.remove(player.getInventory(), (stack) -> stack.isOf(Items.PAPER), 1, false) != 1) {
                return;
            }

            // We don't want to play the sound when the player is uploading a picture, only when it's being taken.
            if (CameraItem.isActive(camera.getRight())) {
                player.getServerWorld().playSoundFromEntity(null, player, CAMERA_SHUTTER, SoundCategory.PLAYERS, 1f, 1f);
            }

            CameraItem.setActive(camera.getRight(), false);
            player.getItemCooldownManager().set(camera.getRight().getItem(), 20 * 3);
            player.swingHand(camera.getLeft(), true);

            player.incrementStat(PICTURES_TAKEN);

            UUID uuid = ServerPictureStore.getInstance().reserveUuid();
            Networking.sendTo(player, new RequestUploadPacket(uuid));
        });

        // Client sends back a picture following a take-picture request
        Map<UUID, ByteCollector> collectors = new ConcurrentHashMap<>();
        Networking.onServerPacketReceive(UploadPartialPicturePacket.class, (packet, player) -> {
            if (!ServerPictureStore.getInstance().isReserved(packet.uuid())) {
                LOGGER.error("{} tried to send a byte section for an unreserved UUID", player.getName().toString());
                return;
            }

            if (packet.bytesLeft() > CONFIG_MANAGER.getConfig().server.maxImageBytes) {
                LOGGER.error("{} sent a picture exceeding the size limit", player.getName().getString());
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveUuid(packet.uuid());
            }

            ByteCollector collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                collectors.remove(uuid);
                ThreadPooler.run(() -> {
                    try {
                        MinecraftServer server = player.getServer();
                        if (server == null) {
                            return;
                        }

                        ServerPictureStore.getInstance().put(server, uuid, new ServerPictureStore.Picture(bytes));
                        ItemStack picture = PictureItem.create(player.getName().getString(), uuid);

                        // We have to do this on a separate thread, because it might spawn an item entity.
                        server.execute(() -> player.getInventory().offerOrDrop(picture));
                    } catch (Exception e) {
                        LOGGER.error("failed to save picture from {}", player.getName().getString(), e);
                        player.sendMessage(Text.translatable("text.camerapture.picture_failed").formatted(Formatting.RED));
                    }
                });
            }));

            if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                LOGGER.error("{} sent a malformed byte section", player.getName().getString());
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveUuid(packet.uuid());
            }

            if (collector.getCurrentLength() > CONFIG_MANAGER.getConfig().server.maxImageBytes) {
                LOGGER.error("{} sent a picture exceeding the size limit", player.getName().getString());
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveUuid(packet.uuid());
            }
        });

        // Client requests a picture with a certain UUID
        Networking.onServerPacketReceive(RequestPicturePacket.class, (packet, player) -> {
            try {
                ServerPictureStore.Picture picture = ServerPictureStore.getInstance().get(player.getServer(), packet.uuid());

                if (picture == null) {
                    LOGGER.warn("{} requested a picture with an unknown UUID", player.getName().getString());
                    Networking.sendTo(player, new PictureErrorPacket(packet.uuid()));
                    return;
                }

                pictureQueue.add(new QueuedPicture(player, packet.uuid(), picture));
            } catch (Exception e) {
                LOGGER.error("failed to load picture for {}", player.getName().getString(), e);
                Networking.sendTo(player, new PictureErrorPacket(packet.uuid()));
            }
        });
    }

    private void registerEvents() {
        // When a player joins, we send them our config.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Config config = CONFIG_MANAGER.getConfig();
            Networking.sendTo(handler.player, new SyncConfigPacket(config.server.maxImageBytes, config.server.maxImageResolution));
        });

        // Every so often, we send a picture from the queue.
        Timer timer = new Timer("Picture Sender");
        Mutable<TimerTask> sendTask = new MutableObject<>();

        // When the server is running, we start the timer that sends the pictures.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    QueuedPicture item = pictureQueue.poll();
                    if (item == null || item.recipient.isDisconnected()) {
                        return;
                    }

                    ByteCollector.split(item.picture.bytes(), SECTION_SIZE, (section, bytesLeft) ->
                            Networking.sendTo(item.recipient, new DownloadPartialPicturePacket(item.uuid, section, bytesLeft)));

                }
            };

            timer.scheduleAtFixedRate(task, 0L, CONFIG_MANAGER.getConfig().server.msPerPicture);
            sendTask.setValue(task);
        });

        // When the server stops, we also stop the timer.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> sendTask.getValue().cancel());
    }

    /**
     * Try to find an (active) camera in a player's hand. Returns null if the player is
     * not holding a camera.
     */
    @Nullable
    public static Pair<Hand, ItemStack> findCamera(PlayerEntity player, boolean active) {
        if (player == null) {
            return null;
        }

        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isOf(Camerapture.CAMERA) && (!active || CameraItem.isActive(stack))) {
                return new Pair<>(hand, stack);
            }
        }

        return null;
    }

    /**
     * Return if the player is currently holding an active camera.
     */
    public static boolean hasActiveCamera(PlayerEntity player) {
        return findCamera(player, true) != null;
    }

    public static Identifier id(String path) {
        //? if >=1.21 {
        /*return Identifier.of("camerapture", path);
        *///?} else
        return new Identifier("camerapture", path);
    }

    private record QueuedPicture(ServerPlayerEntity recipient, UUID uuid, ServerPictureStore.Picture picture) {
    }
}
