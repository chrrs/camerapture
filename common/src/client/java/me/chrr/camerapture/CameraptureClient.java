package me.chrr.camerapture;

import com.luciad.imageio.webp.WebP;
import me.chrr.camerapture.compat.FirstPersonModelCompat;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.clientbound.SyncConfigPacket;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.PictureItemRenderer;

import javax.imageio.ImageIO;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraptureClient {
    public static final float MIN_ZOOM = 1.0f;
    public static final float MAX_ZOOM = 6.0f;

    public static final PictureItemRenderer PICTURE_ITEM_RENDERER = new PictureItemRenderer();

    public static boolean replayModInstalled = false;

    public static void init() {
        ImageIO.scanForPlugins();
        if (!WebP.loadNativeLibrary()) {
            Camerapture.LOGGER.error("failed to load ImageIO-WebP, pictures might not work!");
        }

        ClientPictureStore.getInstance().clear();
        PictureTaker.getInstance().configureFromConfig();
        CameraItem.allowUploading = Camerapture.CONFIG_MANAGER.getConfig().server.allowUploading;

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
            ByteCollector collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                collectors.remove(uuid);
                Camerapture.EXECUTOR.execute(() -> ClientPictureStore.getInstance().processReceivedBytes(uuid, bytes));
            }));

            if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                Camerapture.LOGGER.error("received malformed byte section from server");
                ClientPictureStore.getInstance().processReceivedError(packet.uuid());
            }
        });

        // Server sends back an error following a picture request by UUID
        Camerapture.NETWORK.onReceiveFromServer(PictureErrorPacket.class, (packet) -> {
            ClientPictureStore.getInstance().processReceivedError(packet.uuid());
            collectors.remove(packet.uuid());
        });

        // Server sends over the server-side config
        Camerapture.NETWORK.onReceiveFromServer(SyncConfigPacket.class, (packet) -> {
            PictureTaker.getInstance().configure(packet.maxImageBytes(), packet.maxImageResolution());
            CameraItem.allowUploading = packet.allowUploading();
        });
    }
}
