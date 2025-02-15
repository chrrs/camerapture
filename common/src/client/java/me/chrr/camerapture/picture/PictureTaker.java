package me.chrr.camerapture.picture;

import me.chrr.camerapture.ByteCollector;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.config.Config;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static me.chrr.camerapture.CameraptureClient.MAX_ZOOM;
import static me.chrr.camerapture.CameraptureClient.MIN_ZOOM;

/// This class is responsible for keeping track of taking pictures.
/// It does this by turning off the HUD for a single frame and taking
/// a screenshot. It is also responsible for processing and compressing
/// the image, and making it ready to be sent to the server.
public class PictureTaker {
    private static final PictureTaker INSTANCE = new PictureTaker();

    public float zoomLevel = MIN_ZOOM;

    private boolean hudWasHidden = false;
    private boolean takingPicture = false;

    private BufferedImage picture;

    private int maxImageBytes;
    private int maxImageResolution;

    private PictureTaker() {
    }

    /// Take a screenshot and prepare it, requesting for it to be uploaded.
    public void takePicture() {
        if (takingPicture) {
            return;
        }

        this.takingPicture = true;
        this.hudWasHidden = MinecraftClient.getInstance().options.hudHidden;
        MinecraftClient.getInstance().options.hudHidden = true;
    }

    /// Try reading an image from the file system and prepare it, requesting
    /// for it to be uploaded.
    public void tryUploadFile(Path filePath) {
        try {
            this.picture = ImageIO.read(filePath.toFile());
            Camerapture.NETWORK.sendToServer(new NewPicturePacket());
        } catch (IOException e) {
            Camerapture.LOGGER.error("failed to read picture from file", e);

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.translatable("text.camerapture.upload_failed").formatted(Formatting.RED), false);
            }
        }
    }

    /// Process the frame that has just been rendered, taking a screenshot
    /// and processing it.
    ///
    /// This should only be called on the main renderer thread!
    public void renderTickEnd() {
        if (!this.takingPicture) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();

        // Restore the HUD to the previous state.
        this.takingPicture = false;
        client.options.hudHidden = this.hudWasHidden;

        // Take a screenshot while the HUD was hidden.
        try (NativeImage nativeImage = ScreenshotRecorder.takeScreenshot(client.getFramebuffer())) {
            this.picture = ImageUtil.fromNativeImage(nativeImage, false);
        }

        // Also save the picture as a screenshot if enabled.
        if (Camerapture.CONFIG_MANAGER.getConfig().client.saveScreenshot) {
            ScreenshotRecorder.saveScreenshot(client.runDirectory, client.getFramebuffer(), (text) -> {});
        }

        // We de-activate the camera client-side immediately, to make it feel more responsive.
        CameraItem.HeldCamera activeCamera = CameraItem.find(client.player, true);
        if (activeCamera != null) {
            CameraItem.setActive(activeCamera.stack(), false);
        }

        // Request a new picture ID from the server.
        Camerapture.NETWORK.sendToServer(new NewPicturePacket());
    }

    /// Upload the stored picture to the server when requested, using the specified picture ID.
    public void uploadStoredPicture(UUID pictureId) {
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
                    Camerapture.NETWORK.sendToServer(new UploadPartialPicturePacket(pictureId, section, bytesLeft)));

            // Client-side, we cache the picture directly. This avoids an unnecessary round trip.
            ClientPictureStore.getInstance().processReceivedImage(pictureId, picture);
            ClientPictureStore.getInstance().cacheBytesToDisk(pictureId, bytes);
            this.picture = null;
        } catch (IOException e) {
            Camerapture.LOGGER.error("failed to send picture to server", e);
            this.picture = null;

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                player.sendMessage(Text.translatable("text.camerapture.upload_failed").formatted(Formatting.RED), false);
            }
        }
    }

    public void configureFromConfig() {
        Config config = Camerapture.CONFIG_MANAGER.getConfig();
        this.maxImageBytes = config.server.maxImageBytes;
        this.maxImageResolution = config.server.maxImageResolution;
    }

    public void configure(int maxImageBytes, int maxImageResolution) {
        Camerapture.LOGGER.info("setting max image size to {} bytes, max resolution to {}", maxImageBytes, maxImageResolution);
        this.maxImageBytes = maxImageBytes;
        this.maxImageResolution = maxImageResolution;
    }

    public void zoom(float delta) {
        zoomLevel += delta;
        zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomLevel));
    }

    public float getFovModifier() {
        float zoomProgress = 1f - (zoomLevel - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM);
        return 0.1f + 0.9f * (float) Math.pow(zoomProgress, 2.0);
    }

    public static PictureTaker getInstance() {
        return INSTANCE;
    }
}