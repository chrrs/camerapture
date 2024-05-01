package me.chrr.camerapture.picture;

import me.chrr.camerapture.ByteCollector;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.net.NewPicturePacket;
import me.chrr.camerapture.net.PartialPicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class PictureTaker {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final PictureTaker INSTANCE = new PictureTaker();

    private boolean hudHidden = false;
    private boolean takePicture = false;

    private BufferedImage picture;

    private PictureTaker() {
    }

    public void uploadScreenPicture() {
        if (takePicture) {
            return;
        }

        this.takePicture = true;
        this.hudHidden = MinecraftClient.getInstance().options.hudHidden;
        MinecraftClient.getInstance().options.hudHidden = true;
    }

    public boolean tryUpload(Path filePath) {
        try {
            BufferedImage image = ImageIO.read(filePath.toFile());
            uploadPicture(image);
            return true;
        } catch (IOException e) {
            LOGGER.error("failed to read picture from file", e);
            return false;
        }
    }

    public void uploadPicture(BufferedImage picture) {
        this.picture = picture;

        ClientPlayNetworking.send(new NewPicturePacket());
    }

    public void renderTickEnd() {
        if (!this.takePicture) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        this.takePicture = false;
        client.options.hudHidden = this.hudHidden;

        BufferedImage image;
        try (NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            image = ImageUtil.fromNativeImage(nativeImage, false);
            this.picture = image;
        }

        Pair<Hand, ItemStack> activeCamera = Camerapture.findCamera(client.player, true);
        if (activeCamera != null) {
            CameraItem.setActive(activeCamera.getRight(), false);
        }

        ClientPlayNetworking.send(new NewPicturePacket());
    }

    public void sendStoredPicture(UUID uuid) {
        if (this.picture == null) {
            return;
        }

        try {
            BufferedImage picture = ImageUtil.clampSize(this.picture, Camerapture.MAX_IMAGE_SIZE);

            float factor = 1.0f;
            byte[] bytes = ImageUtil.compressIntoWebP(picture, factor);

            while (bytes.length > Camerapture.MAX_IMAGE_BYTES) {
                if (factor < 0.1f) {
                    throw new IOException("image too big, even at 10% compression (" + bytes.length + " bytes)");
                }

                factor -= 0.05f;
                bytes = ImageUtil.compressIntoWebP(picture, factor);
            }

            LOGGER.debug("sending picture (" + bytes.length + " bytes, " + (int) (factor * 100f) + "%)");
            ByteCollector.split(bytes, Camerapture.SECTION_SIZE, (section, bytesLeft) ->
                    ClientPlayNetworking.send(new PartialPicturePacket(uuid, section, bytesLeft)));

            ClientPictureStore.getInstance().cacheImage(uuid, this.picture);
            this.picture = null;
        } catch (IOException e) {
            LOGGER.error("failed to send picture to server", e);
            this.picture = null;
        }
    }

    public static PictureTaker getInstance() {
        return INSTANCE;
    }
}
