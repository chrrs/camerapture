package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.ThreadPooler;
import me.chrr.camerapture.net.ClientNetworking;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The client-side picture store. This class manages picture son the client side.
 * Its cache is cleared when you leave a world. It also manages caching pictures
 * to disk when that's enabled, and converts them to NativeImages for Minecraft
 * to understand. To the outside, this class works with BufferedImages.
 */
public class ClientPictureStore {
    private static final ClientPictureStore INSTANCE = new ClientPictureStore();

    private final Map<UUID, RemotePicture> pictures = new HashMap<>();

    private ClientPictureStore() {
    }

    public void clear() {
        MinecraftClient.getInstance().executeSync(() -> {
            for (RemotePicture picture : pictures.values()) {
                if (picture.getTextureIdentifier() != null) {
                    MinecraftClient.getInstance()
                            .getTextureManager()
                            .destroyTexture(picture.getTextureIdentifier());
                }
            }

            pictures.clear();
        });
    }

    public void processReceivedError(UUID id) {
        RemotePicture picture = pictures.get(id);
        if (picture == null) {
            return;
        }

        picture.setStatus(RemotePicture.Status.ERROR);
        Camerapture.LOGGER.error("remote error for image {}", id);
    }

    private void fetchPicture(UUID id) {
        ThreadPooler.run(() -> {
            File file = getCacheFilePath(id).toFile();
            if (file.exists()) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    processReceivedImage(id, image);
                    return;
                } catch (IOException e) {
                    // If this fails, we fall through to requesting the picture from the server.
                    Camerapture.LOGGER.error("could not read cached picture {}", id, e);
                }
            }

            ClientNetworking.sendToServer(new RequestDownloadPacket(id));
        });
    }

    public void cacheBytesToDisk(UUID id, byte[] bytes) {
        if (!shouldCacheToDisk()) {
            return;
        }

        try {
            Path path = getCacheFilePath(id);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            Camerapture.LOGGER.error("could not cache picture {}", id, e);
        }
    }

    /**
     * Process a BufferedImage to update the stored picture by UUID.
     */
    public void processReceivedImage(UUID id, BufferedImage image) {
        RemotePicture picture = pictures.computeIfAbsent(id, RemotePicture::new);

        picture.setSize(image.getWidth(), image.getHeight());

        NativeImage nativeImage = ImageUtil.toNativeImage(image);
        NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);

        MinecraftClient.getInstance().executeSync(() -> {
            MinecraftClient.getInstance()
                    .getTextureManager()
                    .registerTexture(picture.getTextureIdentifier(), texture);
            picture.setStatus(RemotePicture.Status.SUCCESS);
        });
    }

    /**
     * Process the bytes received from the server, and update the stored picture.
     */
    public void processReceivedBytes(UUID id, byte[] bytes) {
        try {
            processReceivedImage(id, ImageIO.read(new ByteArrayInputStream(bytes)));
            cacheBytesToDisk(id, bytes);
        } catch (Exception e) {
            Camerapture.LOGGER.error("failed to decode received image bytes for image {}", id, e);
            RemotePicture picture = pictures.computeIfAbsent(id, RemotePicture::new);
            picture.setStatus(RemotePicture.Status.ERROR);
        }
    }

    /**
     * Get a picture by UUID, fetching it from the server if we don't have it yet.
     * This method returns null if the input UUID is null.
     * <p>
     * If the corresponding picture has an error status on the client-side, this
     * method will force-retry fetching the picture from the server.
     */
    public RemotePicture ensureRemotePicture(@NotNull UUID id) {
        RemotePicture picture = pictures.get(id);
        if (picture == null || picture.getStatus() == RemotePicture.Status.ERROR) {
            picture = new RemotePicture(id);
            pictures.put(id, picture);
            fetchPicture(id);
        }

        return picture;
    }

    /**
     * Get a picture by UUID, fetching it from the server if we don't have it yet.
     * This method returns null if the input UUID is null.
     */
    public RemotePicture getServerPicture(@NotNull UUID id) {
        return Optional.ofNullable(pictures.get(id))
                .orElseGet(() -> ensureRemotePicture(id));
    }

    private static boolean shouldCacheToDisk() {
        // We enable single-player picture caching when Replay Mod is installed.
        return CameraptureClient.replayModInstalled
                || (Camerapture.CONFIG_MANAGER.getConfig().client.cachePictures
                && !MinecraftClient.getInstance().isConnectedToLocalServer());
    }

    private Path getCacheFilePath(UUID uuid) {
        Path cacheFolder = FabricLoader.getInstance()
                .getGameDir()
                .resolve("camerapture")
                .resolve("picture-cache");

        return cacheFolder.resolve(uuid + ".webp");
    }

    public static ClientPictureStore getInstance() {
        return INSTANCE;
    }
}
