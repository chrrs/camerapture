package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.ThreadPooler;
import me.chrr.camerapture.net.ClientNetworking;
import me.chrr.camerapture.net.serverbound.RequestPicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
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

    private final Map<UUID, Picture> uuidPictures = new HashMap<>();

    private ClientPictureStore() {
    }

    public void clearCache() {
        MinecraftClient.getInstance().submit(() -> {
            for (Picture picture : uuidPictures.values()) {
                if (picture.identifier != null) {
                    MinecraftClient.getInstance()
                            .getTextureManager()
                            .destroyTexture(picture.identifier);
                }
            }

            uuidPictures.clear();
        });
    }

    public void processReceivedError(UUID uuid) {
        Picture picture = uuidPictures.get(uuid);
        if (picture == null) {
            return;
        }

        picture.setStatus(Status.ERROR);
        Camerapture.LOGGER.error("remote error for image {}", uuid);
    }

    private void fetchPicture(UUID uuid) {
        ThreadPooler.run(() -> {
            File file = getCacheFilePath(uuid).toFile();
            if (file.exists()) {
                try {
                    BufferedImage image = ImageIO.read(file);
                    processImage(uuid, image);
                    return;
                } catch (IOException e) {
                    // If this fails, we fall through to requesting the picture from the server.
                    Camerapture.LOGGER.error("could not read cached picture {}", uuid, e);
                }
            }

            ClientNetworking.sendToServer(new RequestPicturePacket(uuid));
        });
    }

    public void cacheToDisk(UUID uuid, byte[] bytes) {
        if (!shouldCacheToDisk()) {
            return;
        }

        try {
            Path path = getCacheFilePath(uuid);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            Camerapture.LOGGER.error("could not cache picture {}", uuid, e);
        }
    }

    /**
     * Process a BufferedImage to update the stored picture by UUID.
     */
    public void processImage(UUID uuid, BufferedImage image) {
        Picture picture = uuidPictures.computeIfAbsent(uuid, Picture::new);

        NativeImage nativeImage = ImageUtil.toNativeImage(image);
        DynamicTexture texture = new NativeImageBackedTexture(nativeImage);

        picture.setSize(nativeImage.getWidth(), nativeImage.getHeight());

        MinecraftClient.getInstance().submit(() -> {
            MinecraftClient.getInstance()
                    .getTextureManager()
                    .registerTexture(picture.getIdentifier(), (AbstractTexture) texture);
            picture.setStatus(Status.SUCCESS);
        });
    }

    /**
     * Process the bytes received from the server, and update the stored picture.
     */
    public void processReceivedBytes(UUID uuid, byte[] bytes) {
        try {
            processImage(uuid, ImageIO.read(new ByteArrayInputStream(bytes)));
            cacheToDisk(uuid, bytes);
        } catch (Exception e) {
            Camerapture.LOGGER.error("failed to decode received image bytes for image {}", uuid, e);
            Picture picture = uuidPictures.computeIfAbsent(uuid, Picture::new);
            picture.setStatus(Status.ERROR);
        }
    }

    /**
     * Get a picture by UUID, fetching it from the server if we don't have it yet.
     * This method returns null if the input UUID is null.
     * <p>
     * If the corresponding picture has an error status on the client-side, this
     * method will force-retry fetching the picture from the server.
     */
    @NotNull
    public Picture ensureServerPicture(@NotNull UUID uuid) {
        Picture picture = uuidPictures.get(uuid);
        if (picture == null || picture.getStatus() == Status.ERROR) {
            picture = new Picture(uuid);
            uuidPictures.put(uuid, picture);
            fetchPicture(uuid);
        }

        return picture;
    }

    /**
     * Get a picture by UUID, fetching it from the server if we don't have it yet.
     * This method returns null if the input UUID is null.
     */
    public Picture getServerPicture(@NotNull UUID uuid) {
        Picture picture = uuidPictures.get(uuid);
        return Optional.ofNullable(picture).orElseGet(() -> ensureServerPicture(uuid));
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

    public static class Picture {
        private final Identifier identifier;
        private Status status = Status.FETCHING;

        private int width = 0;
        private int height = 0;

        public Picture(Identifier identifier) {
            this.identifier = identifier;
        }

        public Picture(UUID uuid) {
            this(Camerapture.id("pictures/" + uuid));
        }

        public Identifier getIdentifier() {
            return identifier;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

        public Status getStatus() {
            return status;
        }

        public void setSize(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    public enum Status {
        FETCHING,
        SUCCESS,
        ERROR,
    }
}
