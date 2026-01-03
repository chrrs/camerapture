package me.chrr.camerapture.util;

import dev.matrixlab.webp4j.WebPCodec;
import net.minecraft.client.texture.NativeImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.io.IOException;

/// General utility class for working with pictures. Editing picture is
/// mostly done using BufferedImages, as it is easier. Minecraft, however,
/// can only interact with NativeImages, so there's conversion methods.
public enum ImageUtil {
    ;

    /// Convert a {@link BufferedImage} to a {@link NativeImage}.
    public static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, image.getWidth(), image.getHeight(), false);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                nativeImage.setColor(x, y, swapRedAndBlue(image.getRGB(x, y)));
            }
        }

        return nativeImage;
    }

    /// Convert a {@link NativeImage} to a {@link BufferedImage}.
    public static BufferedImage fromNativeImage(NativeImage image) {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[] pixels = image.copyPixelsRgba();

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = swapRedAndBlue(pixels[i]);
        }

        bufferedImage.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        return bufferedImage;
    }

    /// NativeImage's and BufferedImages use different pixel layouts, so we have to swap red and blue.
    private static int swapRedAndBlue(int pixel) {
        return pixel & 0xff00ff00 | ((pixel << 16) & 0xff0000) | ((pixel >> 16) & 0x0000ff);
    }

    /// Shrink a {@link BufferedImage} to be of a maximum dimension in either
    /// direction, while the aspect ratio is kept the same.
    public static BufferedImage clampSize(BufferedImage image, int maxDimension) {
        if (image.getWidth() <= maxDimension && image.getHeight() <= maxDimension) {
            return image;
        }

        int width;
        int height;
        if (image.getWidth() > image.getHeight()) {
            float scale = (float) image.getWidth() / (float) maxDimension;
            width = maxDimension;
            height = (int) (image.getHeight() / scale);
        } else {
            float scale = (float) image.getHeight() / (float) maxDimension;
            width = (int) (image.getWidth() / scale);
            height = maxDimension;
        }

        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaledImage.createGraphics();
        g.drawImage(image.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, width, height, null);
        g.dispose();
        return scaledImage;
    }

    /// Make sure the returned image has a {@link DirectColorModel}, because ImageIO-WebP seems to have bugs
    /// with component color models.
    public static BufferedImage normalize(BufferedImage image) {
        if (image.getColorModel() instanceof DirectColorModel) {
            return image;
        } else {
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = newImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            return newImage;
        }
    }

    /// Write an image into a byte array using WebP, with lossy compression and alpha support.
    public static byte[] compressIntoWebP(BufferedImage image, float quality) throws IOException {
        return WebPCodec.encodeImage(image, quality * 100.0f, false);
    }

    /// Read an image from WebP into a buffered image.
    public static BufferedImage decodeImageFromWebP(byte[] data) throws IOException {
        return WebPCodec.decodeImage(data);
    }
}