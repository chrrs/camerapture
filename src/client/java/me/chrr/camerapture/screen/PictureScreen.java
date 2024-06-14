package me.chrr.camerapture.screen;

import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.util.PictureUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.UUID;

public class PictureScreen extends InGameScreen {
    public static final int BAR_WIDTH = 360;
    public static final int BORDER_THICKNESS = 24;

    private final List<ItemStack> pictures;
    private int index = 0;

    private ClientPictureStore.Picture picture;

    private Text pageNumber;
    private Text customName;

    public PictureScreen(List<ItemStack> pictures) {
        super(Text.translatable("item.camerapture.picture"));
        this.pictures = pictures;

        forceRefresh();
    }

    @Override
    protected void init() {
        super.init();

        if (!isSinglePicture()) {
            int barX = width / 2 - BAR_WIDTH / 2;
            int barY = height - BORDER_THICKNESS - 20;

            addDrawableChild(ButtonWidget.builder(Text.of("←"), button -> {
                        this.index = Math.floorMod(this.index - 1, pictures.size());
                        forceRefresh();
                    })
                    .dimensions(barX, barY, 20, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.of("→"), button -> {
                        this.index = Math.floorMod(this.index + 1, pictures.size());
                        forceRefresh();
                    })
                    .dimensions(barX + BAR_WIDTH - 20, barY, 20, 20)
                    .build());
        }
    }

    @Override
    public void renderScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        // Drawing the item name and page number
        if (!isSinglePicture()) {
            int barY = height - BORDER_THICKNESS - 20 / 2;

            int pageNumberX = width / 2 - this.textRenderer.getWidth(this.pageNumber) / 2;
            if (this.customName != null) {
                int nameX = width / 2 - this.textRenderer.getWidth(this.customName) / 2;
                context.drawText(this.textRenderer, this.customName, nameX, barY - 1 - textRenderer.fontHeight, 0xffffff, false);
                context.drawText(this.textRenderer, this.pageNumber, pageNumberX, barY + 1, 0xffffff, false);
            } else {
                context.drawText(this.textRenderer, this.pageNumber, pageNumberX, barY - textRenderer.fontHeight / 2, 0xffffff, false);
            }
        }

        if (this.picture == null) {
            return;
        }

        // Drawing the picture
        int bottomOffset = isSinglePicture() ? 0 : 24;
        PictureUtil.drawPicture(context, textRenderer, picture, BORDER_THICKNESS, BORDER_THICKNESS, width - BORDER_THICKNESS * 2, height - BORDER_THICKNESS * 2 - bottomOffset);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.index = Math.floorMod(this.index - 1, pictures.size());
            forceRefresh();
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.index = Math.floorMod(this.index + 1, pictures.size());
            forceRefresh();
            return true;
        }

        // We leave the usual handling to last, so we override the arrow keys controlling focus.
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    /*? if >=1.20.4 {*/
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        /*?} else {*//*
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        *//*?}*/
        this.index = Math.floorMod(this.index - (int) verticalAmount, pictures.size());
        forceRefresh();
        return true;
    }

    private void forceRefresh() {
        ItemStack stack = pictures.get(index);
        UUID uuid = PictureItem.getUuid(stack);
        this.picture = ClientPictureStore.getInstance().ensureServerPicture(uuid);

        this.pageNumber = Text.literal((index + 1) + " / " + this.pictures.size()).formatted(Formatting.GRAY);
        this.customName = stack.hasCustomName() ? stack.getName() : null;
    }

    private boolean isSinglePicture() {
        return pictures.size() == 1;
    }
}
