package me.chrr.camerapture.util;

import com.mojang.blaze3d.platform.NativeImage;

import java.awt.image.BufferedImage;

/// Utilities for converting between {@link BufferedImage} and Minecraft's {@link NativeImage}.
public enum NativeImageUtil {
    ;

    /// Convert a {@link BufferedImage} to a {@link NativeImage}.
    public static NativeImage toNativeImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, width, height, false);

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                nativeImage.setPixel(x, y, pixels[row + x]);
            }
        }

        return nativeImage;
    }

    /// Convert a {@link NativeImage} to a {@link BufferedImage}.
    public static BufferedImage fromNativeImage(NativeImage image) {
        int[] pixels = image.getPixels();
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        bufferedImage.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        return bufferedImage;
    }
}
