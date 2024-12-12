package me.chrr.camerapture;

import com.mojang.serialization.Codec;
import me.chrr.camerapture.config.ConfigManager;
import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.gui.AlbumLecternScreenHandler;
import me.chrr.camerapture.gui.AlbumScreenHandler;
import me.chrr.camerapture.gui.PictureFrameScreenHandler;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureCloningRecipe;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.NetworkAdapter;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import me.chrr.camerapture.picture.ServerPictureStore;
import me.chrr.camerapture.picture.StoredPicture;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class Camerapture {
    public static final String MOD_ID = "camerapture";
    public static final Logger LOGGER = LogManager.getLogger();

    public static final Executor EXECUTOR = (runnable) -> new Thread(runnable, "Camerapture Worker").start();
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    public static final PlatformAdapter PLATFORM = ServiceLoader.load(PlatformAdapter.class).iterator().next();
    public static final NetworkAdapter NETWORK = PLATFORM.createNetworkAdapter();

    public static final int SECTION_SIZE = 30_000;

    // Camera
    public static Item CAMERA = new CameraItem();
    public static final SoundEvent CAMERA_SHUTTER = SoundEvent.of(id("camera_shutter"));
    public static final Identifier PICTURES_TAKEN = id("pictures_taken");

    // Picture
    public static Item PICTURE = new PictureItem();
    public static final SpecialRecipeSerializer<PictureCloningRecipe> PICTURE_CLONING =
            new SpecialRecipeSerializer<>(PictureCloningRecipe::new);

    // Album
    public static final Item ALBUM = new AlbumItem();
    public static final ScreenHandlerType<AlbumScreenHandler> ALBUM_SCREEN_HANDLER = new ScreenHandlerType<>(AlbumScreenHandler::new, FeatureSet.empty());
    public static final ScreenHandlerType<AlbumLecternScreenHandler> ALBUM_LECTERN_SCREEN_HANDLER =
            new ScreenHandlerType<>((syncId, playerInventory) -> new AlbumLecternScreenHandler(syncId), FeatureSet.empty());

    // Picture Frame
    public static final EntityType<PictureFrameEntity> PICTURE_FRAME =
            EntityType.Builder.<PictureFrameEntity>create(PictureFrameEntity::new, SpawnGroup.MISC)
                    .maxTrackingRange(10)
                    .build("picture_frame");
    public static final ScreenHandlerType<PictureFrameScreenHandler> PICTURE_FRAME_SCREEN_HANDLER =
            new ScreenHandlerType<>((syncId, pi) -> new PictureFrameScreenHandler(syncId), FeatureSet.empty());

    // Data Components
    public static final ComponentType<PictureItem.PictureData> PICTURE_DATA = ComponentType.<PictureItem.PictureData>builder()
            .codec(PictureItem.PictureData.CODEC).packetCodec(PictureItem.PictureData.PACKET_CODEC)
            .build();
    public static final ComponentType<Boolean> CAMERA_ACTIVE = ComponentType.<Boolean>builder()
            .codec(Codec.BOOL).packetCodec(PacketCodecs.BOOL)
            .build();

    public static void registerPacketHandlers() {
        // Client requests to take / upload a picture
        NETWORK.onReceiveFromClient(NewPicturePacket.class, (packet, player) -> {
            CameraItem.HeldCamera camera = CameraItem.find(player, false);
            if (camera == null) {
                return;
            }

            // If the player is in creative mode, skip taking any paper.
            if (!player.isCreative()) {
                if (Inventories.remove(player.getInventory(), (stack) -> stack.isOf(Items.PAPER), 1, false) != 1) {
                    return;
                }
            }

            // We don't want to play the sound when the player is uploading a picture, only when it's being taken.
            if (CameraItem.isActive(camera.stack())) {
                player.getServerWorld().playSoundFromEntity(null, player, CAMERA_SHUTTER, SoundCategory.PLAYERS, 1f, 1f);
            }

            CameraItem.setActive(camera.stack(), false);
            player.getItemCooldownManager().set(Camerapture.CAMERA, 20 * 3);
            player.swingHand(camera.hand(), true);

            player.incrementStat(PICTURES_TAKEN);

            UUID id = ServerPictureStore.getInstance().reserveId();
            NETWORK.sendToClient(player, new RequestUploadPacket(id));
        });

        // Client sends back a picture following a take-picture request
        Map<UUID, ByteCollector> collectors = new ConcurrentHashMap<>();
        NETWORK.onReceiveFromClient(UploadPartialPicturePacket.class, (packet, player) -> {
            if (!ServerPictureStore.getInstance().isReserved(packet.uuid())) {
                LOGGER.error("{} tried to send a byte section for an unreserved UUID", player.getName().toString());
                return;
            }

            if (packet.bytesLeft() > CONFIG_MANAGER.getConfig().server.maxImageBytes) {
                LOGGER.error("{} sent a picture exceeding the size limit", player.getName().getString());
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveId(packet.uuid());
            }

            ByteCollector collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                collectors.remove(uuid);
                EXECUTOR.execute(() -> {
                    try {
                        MinecraftServer server = player.getServer();
                        if (server == null) {
                            return;
                        }

                        ServerPictureStore.getInstance().put(server, uuid, new StoredPicture(bytes));
                        ItemStack picture = PictureItem.create(player.getName().getString(), uuid);

                        // We have to do this on a separate thread, because it might spawn an item entity.
                        server.execute(() -> player.getInventory().offerOrDrop(picture));
                    } catch (Exception e) {
                        LOGGER.error("failed to save picture from {}", player.getName().getString(), e);
                        player.sendMessage(Text.translatable("text.camerapture.picture_failed").formatted(Formatting.RED), false);
                    }
                });
            }));

            if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                LOGGER.error("{} sent a malformed byte section", player.getName().getString());
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveId(packet.uuid());
            }

            if (collector.getCurrentLength() > CONFIG_MANAGER.getConfig().server.maxImageBytes) {
                LOGGER.error("{} sent a picture exceeding the size limit", player.getName().getString());
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveId(packet.uuid());
            }
        });

        // Client requests a picture with a certain UUID
        NETWORK.onReceiveFromClient(RequestDownloadPacket.class, (packet, player) -> {
            try {
                StoredPicture picture = ServerPictureStore.getInstance().get(player.getServer(), packet.uuid());

                if (picture == null) {
                    LOGGER.warn("{} requested a picture with an unknown UUID", player.getName().getString());
                    NETWORK.sendToClient(player, new PictureErrorPacket(packet.uuid()));
                    return;
                }

                DownloadQueue.getInstance().send(player, packet.uuid(), picture);
            } catch (Exception e) {
                LOGGER.error("failed to load picture for {}", player.getName().getString(), e);
                NETWORK.sendToClient(player, new PictureErrorPacket(packet.uuid()));
            }
        });
    }

    public static Identifier id(String path) {
        return Identifier.of("camerapture", path);
    }
}
