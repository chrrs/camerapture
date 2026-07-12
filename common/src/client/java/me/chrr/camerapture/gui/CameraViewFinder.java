package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.util.CaptureCrop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.text.SimpleDateFormat;
import java.util.Date;

import static me.chrr.camerapture.CameraptureClient.MAX_ZOOM;
import static me.chrr.camerapture.CameraptureClient.MIN_ZOOM;

public enum CameraViewFinder {
    ;

    private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy/MM/dd");
    private static final int LETTERBOX_COLOR = 0x99000000;

    /// Draw the camera view finder to the screen.
    public static void drawCameraViewFinder(GuiGraphicsExtractor graphics, Font font) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        int width = graphics.guiWidth();
        int height = graphics.guiHeight();

        PictureTaker taker = PictureTaker.getInstance();
        taker.aspectRatio = taker.aspectRatio.resolve(width, height);
        double ratio = taker.getCaptureWidthOverHeight(width, height);
        CaptureCrop.Rect crop = CaptureCrop.centerCrop(width, height, ratio);

        drawLetterbox(graphics, width, height, crop);
        drawViewFinder(graphics, crop.x() + 10, crop.y() + 10, crop.x() + crop.width() - 10, crop.y() + crop.height() - 10, 2, 30);
        drawViewFinder(graphics, width / 2 - 20, height / 2 - 20, width / 2 + 20, height / 2 + 20, 1, 10);

        drawZoomBar(graphics, font, Math.min(width - 10, crop.x() + crop.width() - 10), height / 2 - height / 6, height / 3);

        int fh = font.lineHeight;
        int textX = Math.max(25, crop.x() + 15);
        int textY = Math.min(height - 25, crop.y() + crop.height() - 15);

        if (!Camerapture.CONFIG_MANAGER.getConfig().client.simpleCameraHud) {
            graphics.text(font, Component.translatable("text.camerapture.date", SDF_DATE.format(new Date())), textX, textY - fh * 3, CommonColors.WHITE, false);
            graphics.text(font, Component.translatable("text.camerapture.orientation",
                    Component.translatable(taker.orientation.translationKey())), textX, textY - fh * 2, CommonColors.WHITE, false);
            graphics.text(font, Component.translatable("text.camerapture.aspect",
                    Component.translatable(taker.aspectRatio.translationKey(taker.orientation))), textX, textY - fh, CommonColors.WHITE, false);
        }

        if (!CameraItem.canTakePicture(player)) {
            if (System.currentTimeMillis() % 1000 < 500) {
                int w = font.width(Component.translatable("text.camerapture.no_paper"));
                int x = width / 2 - w / 2;
                int y = height / 2 + 32;
                graphics.text(font, Component.translatable("text.camerapture.no_paper"), x, y, CommonColors.RED, false);
            }
        } else if (!Camerapture.CONFIG_MANAGER.getConfig().client.simpleCameraHud) {
            int paper = CameraItem.getPaperInInventory(player);

            Component text = Component.translatable("text.camerapture.paper_available", paper);
            int w = font.width(text);
            int x = Math.min(width - 25 - w, crop.x() + crop.width() - 15 - w);
            int y = Math.min(height - 25 - fh, crop.y() + crop.height() - 15 - fh);
            graphics.text(font, text, x, y, CommonColors.WHITE, false);
        }
    }

    private static void drawLetterbox(GuiGraphicsExtractor graphics, int width, int height, CaptureCrop.Rect crop) {
        if (crop.width() == width && crop.height() == height) {
            return;
        }

        if (crop.x() > 0) {
            graphics.fill(0, 0, crop.x(), height, LETTERBOX_COLOR);
            graphics.fill(crop.x() + crop.width(), 0, width, height, LETTERBOX_COLOR);
        }

        if (crop.y() > 0) {
            graphics.fill(crop.x(), 0, crop.x() + crop.width(), crop.y(), LETTERBOX_COLOR);
            graphics.fill(crop.x(), crop.y() + crop.height(), crop.x() + crop.width(), height, LETTERBOX_COLOR);
        }
    }

    /// Draw four angled brackets, one at each corner of a rectangle.
    private static void drawViewFinder(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int thickness, int length) {
        graphics.fill(x1, y1, x1 + length, y1 + thickness, CommonColors.WHITE);
        graphics.fill(x1, y1, x1 + thickness, y1 + length, CommonColors.WHITE);

        graphics.fill(x2 - length, y1, x2, y1 + thickness, CommonColors.WHITE);
        graphics.fill(x2 - thickness, y1, x2, y1 + length, CommonColors.WHITE);

        graphics.fill(x1, y2 - thickness, x1 + length, y2, CommonColors.WHITE);
        graphics.fill(x1, y2 - length, x1 + thickness, y2, CommonColors.WHITE);

        graphics.fill(x2 - length, y2 - thickness, x2, y2, CommonColors.WHITE);
        graphics.fill(x2 - thickness, y2 - length, x2, y2, CommonColors.WHITE);
    }

    /// Draw a bar that indicates the current zoom level, with a bigger tick on
    /// top of a bar of smaller ticks. Next to the current zoom level is a precise label.
    private static void drawZoomBar(GuiGraphicsExtractor graphics, Font font, int x, int y, int height) {
        // Draw the ticks along the whole zoom bar.
        int ticks = height / 10;
        for (int i = 0; i < ticks; i++) {
            int ty = y + (height * i) / (ticks - 1);
            graphics.fill(x - 6, ty, x, ty + 1, 0xafffffff);
        }

        // Draw a line where the current zoom level is.
        float zoomProgress = 1f - (PictureTaker.getInstance().zoomLevel - MIN_ZOOM) / (MAX_ZOOM - MIN_ZOOM);
        int ty = y + (int) ((float) height * zoomProgress);
        graphics.fill(x - 10, ty - 1, x, ty + 1, CommonColors.WHITE);

        // Show the current zoom level besides the line.
        String zoomLevel = String.format("%.1fx", PictureTaker.getInstance().zoomLevel);
        int textWidth = font.width(zoomLevel);
        graphics.text(font, zoomLevel, x - 12 - textWidth, ty - 4, CommonColors.WHITE, false);
    }
}
