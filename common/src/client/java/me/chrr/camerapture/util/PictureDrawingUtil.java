package me.chrr.camerapture.util;

import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;

public enum PictureDrawingUtil {
    ;

    public static void drawPicture(DrawContext context, TextRenderer textRenderer, RemotePicture picture, int x, int y, int width, int height) {
        switch (picture.getStatus()) {
            case FETCHING -> {
                String loading = LoadingDisplay.get(System.currentTimeMillis());
                Text fetching = Text.translatable("text.camerapture.fetching_picture");
                context.drawCenteredTextWithShadow(textRenderer, fetching, x + width / 2, y + height / 2 - textRenderer.fontHeight, 0xffffff);
                context.drawCenteredTextWithShadow(textRenderer, loading, x + width / 2, y + height / 2, 0x808080);
            }
            case ERROR -> {
                Text error = Text.translatable("text.camerapture.fetching_failed");
                context.drawCenteredTextWithShadow(textRenderer, error, x + width / 2, y + height / 2 - textRenderer.fontHeight / 2, 0xff0000);
            }
            case SUCCESS -> {
                float scaledWidth = (float) width / picture.getWidth();
                float scaleHeight = (float) height / picture.getHeight();

                float scale = Math.min(scaledWidth, scaleHeight);

                int newWidth = (int) (picture.getWidth() * scale);
                int newHeight = (int) (picture.getHeight() * scale);

                int dx = x + width / 2 - newWidth / 2;
                int dy = y + height / 2 - newHeight / 2;

                context.drawTexture(RenderLayer::getGuiTextured, picture.getTextureIdentifier(),
                        dx, dy, 0f, 0f, newWidth, newHeight, newWidth, newHeight);
            }
        }
    }
}