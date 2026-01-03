package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

public class UploadScreen extends Screen {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/upload_picture.png");

    private static final int backgroundWidth = 256;
    private static final int backgroundHeight = 128;

    private PlainTextButton browseButton;

    public UploadScreen() {
        super(Component.translatable("text.camerapture.upload_picture.title").withStyle(ChatFormatting.BOLD));
    }

    @Override
    protected void init() {
        super.init();

        Component text = Component.translatable("text.camerapture.upload_picture.browse").withStyle(ChatFormatting.UNDERLINE);
        int w = font.width(text);

        browseButton = addRenderableWidget(new PlainTextButton(
                this.width / 2 - w / 2, this.height / 2 + font.lineHeight + 4, w,
                font.lineHeight, text, (button) -> browseFile(), font));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        Component description = Component.translatable("text.camerapture.upload_picture.description");

        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - font.lineHeight - 16, CommonColors.WHITE);

        boolean canTakePicture = this.minecraft.player != null && CameraItem.canTakePicture(this.minecraft.player);
        browseButton.visible = canTakePicture;

        if (!canTakePicture) {
            if (System.currentTimeMillis() % 1000 < 500) {
                int y = this.height / 2 + font.lineHeight + 4;
                graphics.drawCenteredString(font, Component.translatable("text.camerapture.no_paper"), this.width / 2, y, CommonColors.RED);
            }
        } else {
            graphics.drawCenteredString(this.font, description, this.width / 2, this.height / 2, CommonColors.WHITE);
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        for (Path path : paths) {
            if (tryUpload(path)) {
                this.onClose();
                return;
            }
        }
    }

    private void browseFile() {
        new Thread(() -> {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer filter = stack.mallocPointer(1);
                filter.put(stack.UTF8("*"));
                filter.flip();

                String path = TinyFileDialogs.tinyfd_openFileDialog("Open Image", "", filter, "Image File", false);
                if (path == null) {
                    return;
                }

                try {
                    if (tryUpload(Path.of(path))) {
                        Minecraft.getInstance().executeIfPossible(this::onClose);
                    }
                } catch (InvalidPathException e) {
                    Camerapture.LOGGER.error("tinyfd returned invalid path", e);
                }
            }
        }).start();
    }

    private boolean tryUpload(Path path) {
        boolean canTakePicture = this.minecraft.player != null && CameraItem.canTakePicture(this.minecraft.player);
        if (!canTakePicture) {
            return false;
        }

        PictureTaker.getInstance().tryUploadFile(path);
        return true;
    }
}