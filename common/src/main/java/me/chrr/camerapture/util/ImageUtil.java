package me.chrr.camerapture.util;

import dev.matrixlab.webp4j.WebPCodec;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.io.IOException;

/// General utility class for working with images and WebP encoding/decoding.
public enum ImageUtil {
    ;

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
            height = Math.max(1, (int) (image.getHeight() / scale));
        } else {
            float scale = (float) image.getHeight() / (float) maxDimension;
            width = Math.max(1, (int) (image.getWidth() / scale));
            height = maxDimension;
        }

        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaledImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(image, 0, 0, width, height, null);
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

    /// Generates a WebP thumbnail from original WebP bytes, scaled to the specified max dimension.
    public static byte[] createThumbnail(byte[] originalWebpBytes, int maxDimension) throws IOException {
        BufferedImage original = decodeImageFromWebP(originalWebpBytes);
        BufferedImage thumb = clampSize(original, maxDimension);
        return compressIntoWebP(thumb, 0.8f);
    }
}
