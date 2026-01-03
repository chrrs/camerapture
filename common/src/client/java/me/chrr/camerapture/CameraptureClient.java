package me.chrr.camerapture;

import me.chrr.camerapture.compat.FirstPersonModelCompat;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.gui.PictureScreen;
import me.chrr.camerapture.gui.UploadScreen;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.clientbound.SyncConfigPacket;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraptureClient {
    public static final float MIN_ZOOM = 1.0f;
    public static final float MAX_ZOOM = 6.0f;

    public static boolean replayModInstalled = false;

    public static SyncedConfig syncedConfig;

    public static void init() {
        ClientPictureStore.getInstance().clear();
        syncedConfig = SyncedConfig.fromServerConfig(Camerapture.CONFIG_MANAGER.getConfig().server);

        if (Camerapture.PLATFORM.isModLoaded("firstperson")) {
            FirstPersonModelCompat.register();
        }

        if (Camerapture.PLATFORM.isModLoaded("replay-mod")) {
            Camerapture.LOGGER.info("Replay Mod is detected, Camerapture will cache pictures, regardless of config.");
            CameraptureClient.replayModInstalled = true;
        }
    }

    public static void registerPacketHandlers() {
        // Server requests client to send over a picture, most likely from the camera
        Camerapture.NETWORK.onReceiveFromServer(RequestUploadPacket.class, (packet) ->
                Camerapture.EXECUTOR.execute(() -> PictureTaker.getInstance().uploadStoredPicture(packet.uuid())));

        // Server sends back a picture following a picture request by UUID
        Map<UUID, ByteCollector> collectors = new ConcurrentHashMap<>();
        Camerapture.NETWORK.onReceiveFromServer(DownloadPartialPicturePacket.class, (packet) -> {
            ByteCollector collector;

            synchronized (collectors) {
                collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                    collectors.remove(uuid);
                    Camerapture.EXECUTOR.execute(() -> ClientPictureStore.getInstance().processReceivedBytes(uuid, bytes));
                }));
            }

            synchronized (collector) {
                if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                    Camerapture.LOGGER.error("received malformed byte section from server");
                    ClientPictureStore.getInstance().processReceivedError(packet.uuid());
                }
            }
        });

        // Server sends back an error following a picture request by UUID
        Camerapture.NETWORK.onReceiveFromServer(PictureErrorPacket.class, (packet) -> {
            ClientPictureStore.getInstance().processReceivedError(packet.uuid());
            collectors.remove(packet.uuid());
        });

        // Server sends over the server-side config
        Camerapture.NETWORK.onReceiveFromServer(SyncConfigPacket.class, (packet) -> {
            Camerapture.LOGGER.info("received synced config: {}", packet.syncedConfig());
            syncedConfig = packet.syncedConfig();
        });
    }

    /// Right-clicking on certain items should open client-side GUI's.
    public static InteractionResult onUseItem(Player player, ItemStack stack) {
        Minecraft client = Minecraft.getInstance();

        if (client.player != player) {
            return InteractionResult.PASS;
        }

        if (stack.is(Camerapture.PICTURE)) {
            // Right-clicking a picture item should open the picture screen.
            if (PictureItem.getPictureData(stack) != null) {
                client.executeIfPossible(() -> client.setScreen(new PictureScreen(List.of(stack))));
                return InteractionResult.SUCCESS;
            }
        } else if (stack.is(Camerapture.ALBUM) && !player.isShiftKeyDown()) {
            // Right-clicking the album should open the gallery screen.
            List<ItemStack> pictures = AlbumItem.getPictures(stack);
            if (!pictures.isEmpty()) {
                client.executeIfPossible(() -> client.setScreen(new PictureScreen(pictures)));
                return InteractionResult.SUCCESS;
            }
        } else if (syncedConfig.permissionLevels().canUpload(player)
                && player.isShiftKeyDown()
                && stack.is(Camerapture.CAMERA)
                && !CameraItem.isActive(stack)
                && !player.getCooldowns().isOnCooldown(stack)) {
            // Shift-right clicking the camera should open the upload screen.
            client.executeIfPossible(() -> client.setScreen(new UploadScreen()));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}
