package me.chrr.camerapture.screen;

import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.LoadingDisplay;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class PictureScreen extends InGameScreen {
    public static final int BORDER_WIDTH = 30;

    private final List<ItemStack> pictures;
    private int index = 0;

    public PictureScreen(List<ItemStack> pictures) {
        super(Text.translatable("item.camerapture.picture"));
        this.pictures = pictures;

        forceRefresh();
    }

    @Override
    public void renderScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        ItemStack stack = pictures.get(index);
        UUID uuid = PictureItem.getUuid(stack);
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (isSinglePicture()) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.index = Math.floorMod(this.index - 1, pictures.size());
            forceRefresh();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.index = Math.floorMod(this.index + 1, pictures.size());
            forceRefresh();
            return true;
        }

        return false;
    }

    private void forceRefresh() {
        ItemStack stack = pictures.get(index);
        UUID uuid = PictureItem.getUuid(stack);
        ClientPictureStore.getInstance().ensureServerPicture(uuid);
    }

    private boolean isSinglePicture() {
        return pictures.size() == 1;
    }
}
