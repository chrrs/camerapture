package me.chrr.camerapture.util;

import com.luciad.imageio.webp.WebPWriteParam;
import net.minecraft.client.texture.NativeImage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * General utility class for working with pictures. Editing picture is
 * mostly done using BufferedImages, as it is easier. Minecraft, however,
 * can only interact with NativeImages, so there's conversion methods.
 */
public class ImageUtil {
    private ImageUtil() {
    }

    private static int swapRedAndBlue(int pixel) {
        return pixel & 0xff00ff00 | ((pixel << 16) & 0xff0000) | ((pixel >> 16) & 0x0000ff);
    }

    public static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(NativeImage.Format.RGBA, image.getWidth(), image.getHeight(), false);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                nativeImage.setColor(x, y, swapRedAndBlue(image.getRGB(x, y)));
            }
        }

        return nativeImage;
    }

    public static BufferedImage fromNativeImage(NativeImage image, boolean hasAlpha) {
        BufferedImage bufferedImage = new BufferedImage(image.getWidth(), image.getHeight(), hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        int[] pixels = image.copyPixelsRgba();

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = swapRedAndBlue(pixels[i]);
        }

        bufferedImage.setRGB(0, 0, image.getWidth(), image.getHeight(), pixels, 0, image.getWidth());
        return bufferedImage;
    }

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

    public static byte[] compressIntoWebP(BufferedImage image, float quality) throws IOException {
        ImageWriter imageWriter = ImageIO.getImageWritersByMIMEType("image/webp").next();

        // We use lossy compression to save space.
        WebPWriteParam writeParam = new WebPWriteParam(imageWriter.getLocale());
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType(writeParam.getCompressionTypes()[WebPWriteParam.LOSSY_COMPRESSION]);
        writeParam.setAlphaCompressionAlgorithm(1);
        writeParam.setCompressionQuality(quality);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream imageOutputStream = new MemoryCacheImageOutputStream(outputStream);

        imageWriter.setOutput(imageOutputStream);
        imageWriter.write(null, new IIOImage(image, null, null), writeParam);
        imageWriter.dispose();

        // We manually flush the ImageOutputStream, because of a bug in the webp-imageio library.
        imageOutputStream.flush();

        return outputStream.toByteArray();
    }
}
