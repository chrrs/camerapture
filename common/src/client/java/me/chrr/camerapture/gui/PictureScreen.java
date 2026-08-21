package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import me.chrr.camerapture.util.PictureDrawingUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.texture.AbstractTexture;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
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

    private Component pageNumber;
    private Component customName;

    private boolean ctrlHeld = false;

    public PictureScreen(List<ItemStack> pictures) {
        super(Component.translatable("item.camerapture.picture"));
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

            addRenderableWidget(Button.builder(Component.nullToEmpty("←"), button -> this.changeIndexBy(-1))
                    .bounds(barX, barY, 20, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.nullToEmpty("→"), button -> this.changeIndexBy(1))
                    .bounds(barX + barWidth - 20, barY, 20, 20)
                    .build());
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        // Drawing the item name and page number
        if (!isSinglePicture()) {
            int barY = height - BORDER_THICKNESS - 20 / 2;

            int pageNumberX = width / 2 - this.font.width(this.pageNumber) / 2;
            if (this.customName != null) {
                int nameX = width / 2 - this.font.width(this.customName) / 2;
                graphics.text(this.font, this.customName, nameX, barY - 1 - font.lineHeight, CommonColors.WHITE, false);
                graphics.text(this.font, this.pageNumber, pageNumberX, barY + 1, CommonColors.WHITE, false);
            } else {
                graphics.text(this.font, this.pageNumber, pageNumberX, barY - font.lineHeight / 2, CommonColors.WHITE, false);
            }
        }

        if (this.picture == null) {
            return;
        }

        if (this.ctrlHeld) {
            Component text = Component.translatable("text.camerapture.save_as").withStyle(ChatFormatting.WHITE);
            int tw = this.font.width(text);
            graphics.text(this.font, text, width / 2 - tw / 2, BORDER_THICKNESS - font.lineHeight - 2, CommonColors.WHITE, false);
        }

        // Drawing the picture
        int bottomOffset = isSinglePicture() ? 0 : 24;
        PictureDrawingUtil.drawPicture(graphics, font, picture, BORDER_THICKNESS, BORDER_THICKNESS, width - BORDER_THICKNESS * 2, height - BORDER_THICKNESS * 2 - bottomOffset);
    }

    @Nullable
    public NativeImage getNativeImage() {
        if (this.minecraft == null ||
                this.picture == null
                || this.picture.getFull().getStatus() != me.chrr.camerapture.picture.PictureTexture.Status.SUCCESS) {
            return null;
        }

        AbstractTexture texture = minecraft.getTextureManager().getTexture(this.picture.getFull().getTextureIdentifier());
        if (!(texture instanceof DynamicTexture backedTexture)) {
            return null;
        }

        return backedTexture.getPixels();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_LEFT_CONTROL) {
            this.ctrlHeld = true;
        } else if (event.key() == GLFW.GLFW_KEY_S && event.hasControlDown()) {
            // On Ctrl-S, we prompt the user to save the image.
            NativeImage image = this.getNativeImage();
            if (image != null) {
                saveAs(image);
                return true;
            }
        } else if (event.key() == GLFW.GLFW_KEY_LEFT) {
            this.changeIndexBy(-1);
            return true;
        } else if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            this.changeIndexBy(1);
            return true;
        }

        // We leave the usual handling to last, so we override the arrow keys controlling focus.
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_LEFT_CONTROL) {
            this.ctrlHeld = false;
        }

        return super.keyReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
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
        this.rebuildWidgets();
        this.forceRefresh();
    }

    private void forceRefresh() {
        this.pageNumber = Component.literal((index + 1) + " / " + this.pictures.size()).withStyle(ChatFormatting.GRAY);
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

        this.customName = stack.get(DataComponents.CUSTOM_NAME);
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
                    image.writeToFile(Path.of(path));
                } catch (IOException e) {
                    Camerapture.LOGGER.error("failed to save picture to disk", e);
                }
            }
        }, "Save prompter").start();
    }
}