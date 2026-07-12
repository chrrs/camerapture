package me.chrr.camerapture;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;

/// Handles on-the-fly capture controls while an active camera is held:
/// - Crouch release toggles landscape/portrait (so you can stay crouched to shoot)
/// - Middle-click / pick-block cycles aspect ratio (and is cancelled)
public final class CameraCaptureControls {
    private static boolean wasSneaking = false;

    private CameraCaptureControls() {
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (CameraItem.find(client.player, true) == null) {
            wasSneaking = client.options.keyShift.isDown();
            return;
        }

        boolean sneaking = client.options.keyShift.isDown();
        if (!sneaking && wasSneaking) {
            PictureTaker.getInstance().toggleOrientation();
        }
        wasSneaking = sneaking;
    }

    /// @return {@code true} if pick-block was consumed by the camera.
    public static boolean onPickBlock() {
        if (CameraItem.find(Minecraft.getInstance().player, true) == null) {
            return false;
        }

        PictureTaker.getInstance().cycleAspectRatio();
        return true;
    }
}
