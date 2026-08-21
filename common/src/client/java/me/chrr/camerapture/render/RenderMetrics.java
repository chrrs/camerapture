package me.chrr.camerapture.render;

import net.minecraft.client.Minecraft;

/// Precomputes and caches global projection metrics across frames to avoid redundant trigonometric
/// and window calculations during block entity extraction. Uses the effective camera FOV for the frame.
public final class RenderMetrics {
    private static int lastWindowHeight = -1;
    private static double lastFov = -1.0;
    private static double cachedFocalLengthPixels = 0.0;

    private RenderMetrics() {
    }

    public static double getFocalLengthPixels() {
        Minecraft client = Minecraft.getInstance();
        int height = client.getWindow().getHeight();

        double fov = 70.0;
        if (client.gameRenderer != null && client.gameRenderer.mainCamera() != null) {
            fov = client.gameRenderer.mainCamera().getFov();
        }
        if (fov <= 0.0 && client.options != null) {
            fov = client.options.fov().get();
        }

        if (height != lastWindowHeight || fov != lastFov) {
            lastWindowHeight = height;
            lastFov = fov;
            double fovRad = Math.toRadians(Math.max(5.0, Math.min(170.0, fov)));
            cachedFocalLengthPixels = (height / 2.0) / Math.tan(fovRad / 2.0);
        }

        return cachedFocalLengthPixels;
    }
}
