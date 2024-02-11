package me.chrr.camerapture.picture;

import me.chrr.camerapture.net.RequestPicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

    public void cacheImage(UUID uuid, BufferedImage image) {
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
            cacheImage(uuid, ImageIO.read(new ByteArrayInputStream(bytes)));
        } catch (Exception e) {
            LOGGER.error("failed to decode received image bytes for image " + uuid, e);
            Picture picture = uuidPictures.computeIfAbsent(uuid, Picture::new);
            picture.setStatus(Status.ERROR);
        }
    }

    public Picture ensureServerPicture(UUID uuid) {
        Picture picture = uuidPictures.get(uuid);
        if (picture == null || picture.getStatus() == Status.ERROR) {
            picture = new Picture(uuid);
            uuidPictures.put(uuid, picture);

            ClientPlayNetworking.send(new RequestPicturePacket(uuid));
        }

        return picture;
    }

    public Picture getServerPicture(UUID uuid) {
        Picture picture = uuidPictures.get(uuid);
        if (picture == null) {
            return ensureServerPicture(uuid);
        } else {
            return picture;
        }
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
            this(new Identifier("camerapture", "pictures/" + uuid));
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
