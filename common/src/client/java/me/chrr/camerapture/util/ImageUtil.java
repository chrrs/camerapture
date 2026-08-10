package me.chrr.camerapture.util;

import com.mojang.blaze3d.platform.NativeImage;
import dev.matrixlab.webp4j.WebPCodec;

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
    ///
    /// This runs for every picture a client loads, so for a world full of posters it's the bulk of the
    /// cost of a picture appearing. Two things matter here:
    ///
    /// Pixels are read in one bulk call rather than per pixel. `BufferedImage#getRGB(int, int)` goes
    /// through the raster and colour model on every single call, whereas the bulk overload hands off to
    /// an optimised path.
    ///
    /// The loop then walks rows, not columns. Both the source array and NativeImage's buffer are laid
    /// out row-major (`(x + y * width) * 4`), so iterating x in the outer loop — as this did — jumped a
    /// whole row's stride on every step and missed cache on essentially every write.
    ///
    /// The writes stay per-pixel because NativeImage exposes no bulk setter.
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