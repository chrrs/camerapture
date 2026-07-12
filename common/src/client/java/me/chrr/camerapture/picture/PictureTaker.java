package me.chrr.camerapture.picture;

import me.chrr.camerapture.ByteCollector;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import me.chrr.camerapture.util.ImageUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

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
    public CameraOrientation orientation = CameraOrientation.LANDSCAPE;
    public CameraAspectRatio aspectRatio = CameraAspectRatio.SCREEN;

    private boolean hudWasHidden = false;
    private boolean takingPicture = false;

    private BufferedImage picture;

    private PictureTaker() {
    }

    public void toggleOrientation() {
        this.orientation = this.orientation.next();
    }

    public void cycleAspectRatio() {
        this.aspectRatio = this.aspectRatio.next();
    }

    public double getCaptureWidthOverHeight(int width, int height) {
        return this.aspectRatio.widthOverHeight(this.orientation, width, height);
    }

    /// Take a screenshot and prepare it, requesting for it to be uploaded.
    public void takePicture() {
        if (takingPicture) {
            return;
        }

        if (!Camerapture.PLATFORM.canTakePicture()) {
            return;
        }

        this.takingPicture = true;
        this.hudWasHidden = Minecraft.getInstance().options.hideGui;
        Minecraft.getInstance().options.hideGui = true;
    }

    /// Try reading an image from the file system and prepare it, requesting
    /// for it to be uploaded.
    public void tryUploadFile(Path filePath) {
        try {
            this.picture = ImageIO.read(filePath.toFile());
            Camerapture.NETWORK.sendToServer(new NewPicturePacket());
        } catch (Exception e) {
            Camerapture.LOGGER.error("failed to read picture from file", e);

            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("text.camerapture.upload_failed").withStyle(ChatFormatting.RED));
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

        Minecraft client = Minecraft.getInstance();

        // Restore the HUD to the previous state.
        this.takingPicture = false;
        client.options.hideGui = this.hudWasHidden;

        // Save the picture as a screenshot if enabled.
        if (Camerapture.CONFIG_MANAGER.getConfig().client.saveScreenshot) {
            Screenshot.grab(client.gameDirectory, client.getMainRenderTarget(), (text) -> {
            });
        }

        // We de-activate the camera client-side immediately, to make it feel more responsive.
        CameraItem.HeldCamera activeCamera = CameraItem.find(client.player, true);
        if (activeCamera != null) {
            CameraItem.setActive(activeCamera.stack(), false);
        }

        // Take a screenshot while the HUD was hidden, then crop to the selected aspect ratio.
        Screenshot.takeScreenshot(client.getMainRenderTarget(), (nativeImage) -> {
            BufferedImage full = ImageUtil.fromNativeImage(nativeImage);
            nativeImage.close();

            double ratio = getCaptureWidthOverHeight(full.getWidth(), full.getHeight());
            this.picture = ImageUtil.cropToAspect(full, ratio);

            // Request a new picture ID from the server.
            Camerapture.NETWORK.sendToServer(new NewPicturePacket());
        });
    }

    /// Upload the stored picture to the server when requested, using the specified picture ID.
    public void uploadStoredPicture(UUID pictureId) {
        if (this.picture == null) {
            Camerapture.LOGGER.error("server requested a picture, but we don't have any stored");
            return;
        }

        try {
            BufferedImage picture = ImageUtil.clampSize(this.picture,
                    CameraptureClient.syncedConfig.maxImageResolution());
            picture = ImageUtil.normalize(picture);

            // Starting at 100% quality, we step up the compression by 5% each time
            // until we fit it into our size limit.
            float factor = 1.0f;
            byte[] bytes = ImageUtil.compressIntoWebP(picture, factor);

            while (bytes.length > CameraptureClient.syncedConfig.maxImageBytes()) {
                if (factor < 0.1f) {
                    throw new IOException("image too big, even at 10% compression (" + bytes.length + " bytes)");
                }

                factor -= 0.05f;
                bytes = ImageUtil.compressIntoWebP(picture, factor);
            }

            Camerapture.LOGGER.debug("sending picture ({} bytes, {}%)", bytes.length, (int) (factor * 100f));
            ByteCollector.split(bytes, Camerapture.CLIENT_SECTION_SIZE, (section, bytesLeft) ->
                    Camerapture.NETWORK.sendToServer(new UploadPartialPicturePacket(pictureId, section, bytesLeft)));

            // Client-side, we cache the picture directly. This avoids an unnecessary round trip.
            ClientPictureStore.getInstance().processReceivedImage(pictureId, picture);
            ClientPictureStore.getInstance().cacheBytesToDisk(pictureId, bytes);
            this.picture = null;
        } catch (Exception e) {
            Camerapture.LOGGER.error("failed to send picture to server", e);
            this.picture = null;

            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.sendSystemMessage(Component.translatable("text.camerapture.upload_failed").withStyle(ChatFormatting.RED));
            }
        }
    }

    public void zoom(float delta) {
        zoomLevel += delta;
        zoomLevel = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoomLevel));
    }

    public float getFovModifier() {
        float zoomProgress = (zoomLevel - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM);
        return 0.1f + 0.9f * (float) Math.pow(1f - zoomProgress, 2.0);
    }

    public float getSensitivityModifier() {
        float zoomProgress = (zoomLevel - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM);
        float multiplier = 1f - Camerapture.CONFIG_MANAGER.getConfig().client.zoomMouseSensitivity;
        return 1f - zoomProgress * multiplier;
    }

    public static PictureTaker getInstance() {
        return INSTANCE;
    }
}