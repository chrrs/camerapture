package me.chrr.camerapture.screen;

import me.chrr.camerapture.picture.ClientPictureStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.text.Text;

import java.util.UUID;

public class PictureScreen extends InGameScreen {
    public static final int BORDER_WIDTH = 30;

    private final UUID uuid;

    public PictureScreen(UUID uuid) {
        super(Text.translatable("item.camerapture.picture"));

        this.uuid = uuid;
        ClientPictureStore.getInstance().ensureServerPicture(uuid);
    }

    @Override
    public void renderScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        ClientPictureStore.Picture picture = ClientPictureStore.getInstance().getServerPicture(uuid);

        if (picture == null) {
            return;
        }

        switch (picture.getStatus()) {
            case FETCHING -> {
                String loading = LoadingDisplay.get(System.currentTimeMillis());
                Text fetching = Text.translatable("text.camerapture.fetching_picture");
                context.drawCenteredTextWithShadow(textRenderer, fetching, width / 2, height / 2 - textRenderer.fontHeight, 0xffffff);
                context.drawCenteredTextWithShadow(textRenderer, loading, width / 2, height / 2, 0x808080);
            }
            case ERROR -> {
                Text error = Text.translatable("text.camerapture.fetching_failed");
                context.drawCenteredTextWithShadow(textRenderer, error, width / 2, height / 2 - textRenderer.fontHeight / 2, 0xff0000);
            }
            case SUCCESS -> {
                int maxWidth = width - BORDER_WIDTH * 2;
                int maxHeight = height - BORDER_WIDTH * 2;

                float scaledWidth = (float) maxWidth / picture.getWidth();
                float scaleHeight = (float) maxHeight / picture.getHeight();

                float scale = Math.min(scaledWidth, scaleHeight);

                int newWidth = (int) (picture.getWidth() * scale);
                int newHeight = (int) (picture.getHeight() * scale);

                int x = width / 2 - newWidth / 2;
                int y = height / 2 - newHeight / 2;

                context.drawTexture(picture.getIdentifier(), x, y, 0f, 0f, newWidth, newHeight, newWidth, newHeight);
            }
        }
    }
}
