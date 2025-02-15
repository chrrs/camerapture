package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import me.chrr.camerapture.util.PictureDrawingUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PictureScreen extends Screen {
    public static final int MAX_BAR_WIDTH = 360;
    public static final int BORDER_THICKNESS = 24;

    private List<ItemStack> pictures;
    private int index = 0;

    private RemotePicture picture;

    private Text pageNumber;
    private Text customName;

    private boolean ctrlHeld = false;

    public PictureScreen(List<ItemStack> pictures) {
        super(Text.translatable("item.camerapture.picture"));
        this.pictures = pictures;

        forceRefresh();
    }

    @Override
    protected void init() {
        super.init();

        if (!isSinglePicture()) {
            int barWidth = Math.min(MAX_BAR_WIDTH, width - BORDER_THICKNESS * 2);

            int barX = width / 2 - barWidth / 2;
            int barY = height - BORDER_THICKNESS - 20;

            addDrawableChild(ButtonWidget.builder(Text.of("←"), button -> this.changeIndexBy(-1))
                    .dimensions(barX, barY, 20, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.of("→"), button -> this.changeIndexBy(1))
                    .dimensions(barX + barWidth - 20, barY, 20, 20)
                    .build());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

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

        if (this.ctrlHeld) {
            Text text = Text.translatable("text.camerapture.save_as").formatted(Formatting.GRAY);
            int tw = this.textRenderer.getWidth(text);
            context.drawText(this.textRenderer, text, width / 2 - tw / 2, BORDER_THICKNESS - textRenderer.fontHeight - 2, 0xffffff, false);
        }

        // Drawing the picture
        int bottomOffset = isSinglePicture() ? 0 : 24;
        PictureDrawingUtil.drawPicture(context, textRenderer, picture, BORDER_THICKNESS, BORDER_THICKNESS, width - BORDER_THICKNESS * 2, height - BORDER_THICKNESS * 2 - bottomOffset);
    }

    @Nullable
    public NativeImage getNativeImage() {
        if (this.client == null ||
                this.picture == null
                || this.picture.getStatus() != RemotePicture.Status.SUCCESS) {
            return null;
        }

        AbstractTexture texture = client.getTextureManager().getTexture(this.picture.getTextureIdentifier());
        if (!(texture instanceof NativeImageBackedTexture backedTexture)) {
            return null;
        }

        return backedTexture.getImage();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) {
            this.ctrlHeld = true;
        } else if (keyCode == GLFW.GLFW_KEY_S && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            // On Ctrl-S, we prompt the user to save the image.
            NativeImage image = this.getNativeImage();
            if (image != null) {
                saveAs(image);
                return true;
            }
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            this.changeIndexBy(-1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            this.changeIndexBy(1);
            return true;
        }

        // We leave the usual handling to last, so we override the arrow keys controlling focus.
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) {
            this.ctrlHeld = false;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    //? if >=1.20.4 {
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        //?} else
        /*public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {*/
        this.changeIndexBy((int) -verticalAmount);
        return true;
    }

    public void changeIndexBy(int delta) {
        if (!this.pictures.isEmpty()) {
            this.index = Math.floorMod(this.index + delta, pictures.size());
            forceRefresh();
        }
    }

    public void setPictures(List<ItemStack> pictures) {
        this.pictures = pictures;
        this.index = 0;
        this.clearAndInit();
        this.forceRefresh();
    }

    private void forceRefresh() {
        this.pageNumber = Text.literal((index + 1) + " / " + this.pictures.size()).formatted(Formatting.GRAY);
        if (this.index >= this.pictures.size()) {
            this.picture = null;
            this.customName = null;
            return;
        }

        ItemStack stack = pictures.get(index);
        PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
        if (pictureData == null) {
            return;
        }

        this.picture = ClientPictureStore.getInstance().ensureRemotePicture(pictureData.id());

        this.customName = stack.get(DataComponentTypes.CUSTOM_NAME);
    }

    private boolean isSinglePicture() {
        return pictures.size() == 1;
    }

    private void saveAs(NativeImage image) {
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filter = stack.mallocPointer(1);
                filter.put(stack.UTF8("png"));
                filter.flip();

                String path = TinyFileDialogs.tinyfd_saveFileDialog("Save Picture", "picture.png", filter, "*.png");
                if (path == null) {
                    return;
                }

                try {
                    image.writeTo(Path.of(path));
                } catch (IOException e) {
                    Camerapture.LOGGER.error("failed to save picture to disk", e);
                }
            }
        }, "Save prompter").start();
    }
}