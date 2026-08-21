package me.chrr.camerapture.render;

import net.minecraft.client.Minecraft;

/// Precomputes and caches global projection metrics across frames to avoid redundant trigonometric
/// and window calculations during block entity extraction.
public final class RenderMetrics {
    private static int lastWindowHeight = -1;
    private static double lastFov = -1.0;
    private static double cachedFocalLengthPixels = 0.0;

    private RenderMetrics() {
    }

    public static double getFocalLengthPixels() {
        Minecraft client = Minecraft.getInstance();
        int height = client.getWindow().getHeight();
        double fov = client.options.fov().get();

        if (height != lastWindowHeight || fov != lastFov) {
            lastWindowHeight = height;
            lastFov = fov;
            double fovRad = Math.toRadians(fov);
            cachedFocalLengthPixels = (height / 2.0) / Math.tan(fovRad / 2.0);
        }

        return cachedFocalLengthPixels;
    }
}
