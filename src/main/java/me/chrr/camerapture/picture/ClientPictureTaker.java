package me.chrr.camerapture.picture;

import me.chrr.camerapture.ThreadManager;
import me.chrr.camerapture.net.PicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ClientPictureTaker {
    public static final int MAX_IMAGE_SIZE = 1280;

    private static final Logger LOGGER = LogManager.getLogger();
    private static final ClientPictureTaker INSTANCE = new ClientPictureTaker();

    private boolean hudHidden = false;
    private UUID takeUuid;

    private ClientPictureTaker() {
    }

    public void takePicture(UUID uuid) {
        if (takeUuid != null) {
            return;
        }

        this.takeUuid = uuid;
        this.hudHidden = MinecraftClient.getInstance().options.hudHidden;
        MinecraftClient.getInstance().options.hudHidden = true;
    }

    public void renderTickEnd() {
        if (this.takeUuid == null) {
            return;
        }

        UUID uuid = this.takeUuid;
        this.takeUuid = null;
        MinecraftClient.getInstance().options.hudHidden = this.hudHidden;

        BufferedImage image;
        try (NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(MinecraftClient.getInstance().getFramebuffer())) {
            image = ImageUtil.fromNativeImage(nativeImage, false);
            ThreadManager.getInstance().run(() -> sendPicture(uuid, image));
        }
    }

    private void sendPicture(UUID uuid, BufferedImage image) {
        try {
            BufferedImage picture = ImageUtil.clampSize(image, MAX_IMAGE_SIZE);

            float factor = 1.0f;
            byte[] bytes = ImageUtil.compressIntoJpg(picture, factor);

            while (bytes.length > ServerImageStore.MAX_BYTES) {
                if (factor < 0.1f) {
                    throw new IOException("image too big, even at 10% compression (" + bytes.length + " bytes)");
                }

                factor -= 0.05f;
                bytes = ImageUtil.compressIntoJpg(picture, factor);
            }

            LOGGER.debug("sending picture (" + bytes.length + " bytes, " + (int) (factor * 100f) + "%)");
            ClientPlayNetworking.send(new PicturePacket(uuid, bytes));

            ClientPictureStore.getInstance().cacheImage(uuid, image);
        } catch (IOException e) {
            LOGGER.error("failed to send picture to server", e);
        }
    }

    public static ClientPictureTaker getInstance() {
        return INSTANCE;
    }
}
