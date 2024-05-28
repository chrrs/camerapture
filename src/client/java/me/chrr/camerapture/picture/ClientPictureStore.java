package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.ThreadPooler;
import me.chrr.camerapture.net.RequestPicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClientPictureStore {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ClientPictureStore INSTANCE = new ClientPictureStore();

    private final Map<UUID, Picture> uuidPictures = new HashMap<>();

    private ClientPictureStore() {
    }

    public void clearCache() {
        uuidPictures.clear();
    }

    public void processReceivedError(UUID uuid) {
        Picture picture = uuidPictures.get(uuid);
        if (picture == null) {
            return;
        }

        picture.setStatus(Status.ERROR);
        LOGGER.error("remote error for image " + uuid);
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
                    LOGGER.error("could not read cached picture " + uuid, e);
                }
            }

            ClientPlayNetworking.send(new RequestPicturePacket(uuid));
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
            LOGGER.error("could not cache picture " + uuid, e);
        }
    }

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

    public void processReceivedBytes(UUID uuid, byte[] bytes) {
        try {
            processImage(uuid, ImageIO.read(new ByteArrayInputStream(bytes)));
            cacheToDisk(uuid, bytes);
        } catch (Exception e) {
            LOGGER.error("failed to decode received image bytes for image " + uuid, e);
            Picture picture = uuidPictures.computeIfAbsent(uuid, Picture::new);
            picture.setStatus(Status.ERROR);
        }
    }

    @Nullable
    public Picture ensureServerPicture(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        Picture picture = uuidPictures.get(uuid);
        if (picture == null || picture.getStatus() == Status.ERROR) {
            picture = new Picture(uuid);
            uuidPictures.put(uuid, picture);
            fetchPicture(uuid);
        }

        return picture;
    }

    @Nullable
    public Picture getServerPicture(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        Picture picture = uuidPictures.get(uuid);
        if (picture == null) {
            return ensureServerPicture(uuid);
        } else {
            return picture;
        }
    }

    private static boolean shouldCacheToDisk() {
        // We enable single-player picture caching when Replay Mod is installed.
        return !MinecraftClient.getInstance().isConnectedToLocalServer()
                || CameraptureClient.shouldCacheLocalWorlds;
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
