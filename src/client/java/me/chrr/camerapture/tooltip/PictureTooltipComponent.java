package me.chrr.camerapture.tooltip;

import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;

import java.util.UUID;

public class PictureTooltipComponent implements TooltipComponent {
    private static final int PICTURE_HEIGHT = 50;

    private final RemotePicture picture;

    public PictureTooltipComponent(UUID pictureId) {
        this.picture = ClientPictureStore.getInstance().getServerPicture(pictureId);
    }

    @Override
    public int getHeight() {
        return picture.getStatus() == RemotePicture.Status.SUCCESS ? PICTURE_HEIGHT + 4 : 0;
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return picture.getStatus() == RemotePicture.Status.SUCCESS
                ? (int) ((float) PICTURE_HEIGHT / (float) picture.getHeight() * (float) picture.getWidth())
                : 0;
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context) {
        if (picture.getStatus() != RemotePicture.Status.SUCCESS) {
            return;
        }

        int width = (int) ((float) PICTURE_HEIGHT / (float) picture.getHeight() * (float) picture.getWidth());
        context.drawTexture(picture.getTextureIdentifier(), x, y, 400, 0f, 0f, width, PICTURE_HEIGHT, width, PICTURE_HEIGHT);
    }
}
