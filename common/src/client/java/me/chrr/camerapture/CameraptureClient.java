package me.chrr.camerapture;

import com.luciad.imageio.webp.WebP;
import com.luciad.imageio.webp.WebPImageReaderSpi;
import com.luciad.imageio.webp.WebPImageWriterSpi;
import me.chrr.camerapture.compat.FirstPersonModelCompat;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.clientbound.SyncConfigPacket;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.PictureItemRenderer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraptureClient {
    public static final float MIN_ZOOM = 1.0f;
    public static final float MAX_ZOOM = 6.0f;

    public static final PictureItemRenderer PICTURE_ITEM_RENDERER = new PictureItemRenderer();

    public static boolean replayModInstalled = false;

    public static SyncedConfig syncedConfig;

    public static void init() {
        loadImageIOWebP();

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

    private static void loadImageIOWebP() {
        ImageIO.scanForPlugins();
        if (!WebP.loadNativeLibrary()) {
            Camerapture.LOGGER.error("failed to load ImageIO-WebP, pictures might not work!");
        }

        boolean readerFound = false;
        Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("webp");
        while (readers.hasNext())
            readerFound |= readers.next().getOriginatingProvider() instanceof WebPImageReaderSpi;

        if (!readerFound) {
            Camerapture.LOGGER.error("WebP image reader not found, loading pictures might not work!");
        }

        boolean writerFound = false;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        while (writers.hasNext())
            writerFound |= writers.next().getOriginatingProvider() instanceof WebPImageWriterSpi;

        if (!writerFound) {
            Camerapture.LOGGER.error("WebP image writer not found, taking pictures might not work!");
        }

        if (readerFound && writerFound) {
            Camerapture.LOGGER.info("successfully loaded WebP image reader and writer!");
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
            Camerapture.LOGGER.info("received synced config: {}", packet.syncedConfig());
            syncedConfig = packet.syncedConfig();
        });
    }
}
