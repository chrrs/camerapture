package me.chrr.camerapture.util;

import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.network.chat.Component;

public enum PictureDrawingUtil {
    ;

    public static void drawPicture(GuiGraphicsExtractor graphics, Font font, RemotePicture picture, int x, int y, int width, int height) {
        switch (picture.getStatus()) {
            case FETCHING -> {
                String loading = LoadingDotsText.get(System.currentTimeMillis());
                Component fetching = Component.translatable("text.camerapture.fetching_picture");
                graphics.centeredText(font, fetching, x + width / 2, y + height / 2 - font.lineHeight, 0xffffff);
                graphics.centeredText(font, loading, x + width / 2, y + height / 2, 0x808080);
            }
            case ERROR -> {
                Component error = Component.translatable("text.camerapture.fetching_failed");
                graphics.centeredText(font, error, x + width / 2, y + height / 2 - font.lineHeight / 2, 0xff0000);
            }
            case SUCCESS -> {
                float scaledWidth = (float) width / picture.getWidth();
                float scaleHeight = (float) height / picture.getHeight();

                float scale = Math.min(scaledWidth, scaleHeight);

                int newWidth = (int) (picture.getWidth() * scale);
                int newHeight = (int) (picture.getHeight() * scale);

                int dx = x + width / 2 - newWidth / 2;
                int dy = y + height / 2 - newHeight / 2;

                graphics.blit(RenderPipelines.GUI_TEXTURED, picture.getTextureIdentifier(),
                        dx, dy, 0f, 0f, newWidth, newHeight, newWidth, newHeight);
            }
        }
    }
}