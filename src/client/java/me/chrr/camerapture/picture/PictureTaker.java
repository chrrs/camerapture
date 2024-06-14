package me.chrr.camerapture.picture;

import me.chrr.camerapture.ByteCollector;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.config.Config;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.net.NewPicturePacket;
import me.chrr.camerapture.net.PartialPicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

/**
 * This class is responsible for keeping track of taking pictures.
 * It does this by turning off the HUD for a single frame and taking
 * a screenshot. It is also responsible for processing and compressing
 * the image, and making it ready to be sent to the server.
 */
public class PictureTaker {
    private static final PictureTaker INSTANCE = new PictureTaker();

    private boolean hudHidden = false;
    private boolean takePicture = false;

    private BufferedImage picture;

    private int maxImageBytes;
    private int maxImageResolution;

    private PictureTaker() {
    }

    /**
     * Take a screenshot and prepare it, requesting for it to be uploaded.
     */
    public void uploadScreenPicture() {
        if (takePicture) {
            return;
        }

        this.takePicture = true;
        this.hudHidden = MinecraftClient.getInstance().options.hudHidden;
        MinecraftClient.getInstance().options.hudHidden = true;
    }

    /**
     * Try reading an image from the file system and prepare it, requesting
     * for it to be uploaded.
     */
    public boolean tryUploadFile(Path filePath) {
        try {
            this.picture = ImageIO.read(filePath.toFile());
            ClientPlayNetworking.send(new NewPicturePacket());
            return true;
        } catch (IOException e) {
            Camerapture.LOGGER.error("failed to read picture from file", e);
            return false;
        }
    }

    /**
     * Process the frame that has just been rendered, taking a screenshot
     * and processing it.
     * <p>
     * This should only be called on the main renderer thread!
     */
    public void renderTickEnd() {
        if (!this.takePicture) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        this.takePicture = false;
        client.options.hudHidden = this.hudHidden;

        try (NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            this.picture = ImageUtil.fromNativeImage(nativeImage, false);
        }

        // We de-activate the camera client-side immediately, to make it feel more responsive.
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
            BufferedImage picture = ImageUtil.clampSize(this.picture, maxImageResolution);

            // Starting at 100% quality, we step up the compression by 5% each time
            // until we fit it into our size limit.
            float factor = 1.0f;
            byte[] bytes = ImageUtil.compressIntoWebP(picture, factor);

            while (bytes.length > maxImageBytes) {
                if (factor < 0.1f) {
                    throw new IOException("image too big, even at 10% compression (" + bytes.length + " bytes)");
                }

                factor -= 0.05f;
                bytes = ImageUtil.compressIntoWebP(picture, factor);
            }

            Camerapture.LOGGER.debug("sending picture ({} bytes, {}%)", bytes.length, (int) (factor * 100f));
            ByteCollector.split(bytes, Camerapture.SECTION_SIZE, (section, bytesLeft) ->
                    ClientPlayNetworking.send(new PartialPicturePacket(uuid, section, bytesLeft)));

            // Client-side, we cache the picture directly. This avoids an unnecessary round trip.
            ClientPictureStore.getInstance().processImage(uuid, picture);
            ClientPictureStore.getInstance().cacheToDisk(uuid, bytes);
            this.picture = null;
        } catch (IOException e) {
            Camerapture.LOGGER.error("failed to send picture to server", e);
            this.picture = null;

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.translatable("text.camerapture.upload_failed").formatted(Formatting.RED));
            }
        }
    }

    public void resetConfig() {
        Config config = Camerapture.CONFIG_MANAGER.getConfig();
        this.maxImageBytes = config.server.maxImageBytes;
        this.maxImageResolution = config.server.maxImageResolution;
    }

    public void setConfig(int maxImageBytes, int maxImageResolution) {
        Camerapture.LOGGER.info("setting max image size to {} bytes, max resolution to {}", maxImageBytes, maxImageResolution);
        this.maxImageBytes = maxImageBytes;
        this.maxImageResolution = maxImageResolution;
    }

    public static PictureTaker getInstance() {
        return INSTANCE;
    }
}
