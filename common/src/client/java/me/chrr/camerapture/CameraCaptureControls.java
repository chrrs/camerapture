package me.chrr.camerapture;

import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;

/// Handles on-the-fly capture controls while an active camera is held:
/// - A quick crouch tap toggles landscape/portrait; holding crouch to shoot does not
/// - Middle-click / pick-block cycles aspect ratio (and is cancelled)
public final class CameraCaptureControls {
    /// Crouch holds longer than this are treated as framing, not an orientation tap.
    private static final long ORIENTATION_TAP_MAX_MS = 250L;

    private static boolean wasSneaking = false;
    private static long sneakPressedAtMs = 0L;

    private CameraCaptureControls() {
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (CameraItem.find(client.player, true) == null) {
            wasSneaking = client.options.keyShift.isDown();
            sneakPressedAtMs = 0L;
            return;
        }

        boolean sneaking = client.options.keyShift.isDown();
        if (sneaking && !wasSneaking) {
            sneakPressedAtMs = System.currentTimeMillis();
        } else if (!sneaking && wasSneaking) {
            if (sneakPressedAtMs > 0L
                    && System.currentTimeMillis() - sneakPressedAtMs <= ORIENTATION_TAP_MAX_MS) {
                PictureTaker.getInstance().toggleOrientation();
            }
            sneakPressedAtMs = 0L;
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
