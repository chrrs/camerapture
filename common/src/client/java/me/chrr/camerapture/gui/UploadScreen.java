package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.PressableTextWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
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

    private PressableTextWidget browseButton;

    public UploadScreen() {
        super(Text.translatable("text.camerapture.upload_picture.title").formatted(Formatting.BOLD));
    }

    @Override
    protected void init() {
        super.init();

        Text text = Text.translatable("text.camerapture.upload_picture.browse").formatted(Formatting.UNDERLINE);
        int w = textRenderer.getWidth(text);

        browseButton = addDrawableChild(new PressableTextWidget(
                this.width / 2 - w / 2, this.height / 2 + textRenderer.fontHeight + 4, w,
                textRenderer.fontHeight, text, (button) -> browseFile(), textRenderer));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        Text description = Text.translatable("text.camerapture.upload_picture.description");

        context.drawTexture(TEXTURE, width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - textRenderer.fontHeight - 16, 0xffffff);

        boolean canTakePicture = this.client != null && this.client.player != null && CameraItem.canTakePicture(this.client.player);
        browseButton.visible = canTakePicture;

        if (!canTakePicture) {
            if (System.currentTimeMillis() % 1000 < 500) {
                int y = this.height / 2 + textRenderer.fontHeight + 4;
                context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.camerapture.no_paper"), this.width / 2, y, 0xffff0000);
            }
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, description, this.width / 2, this.height / 2, 0xffffff);
        }
    }

    @Override
    public void filesDragged(List<Path> paths) {
        for (Path path : paths) {
            if (tryUpload(path)) {
                this.close();
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
                        MinecraftClient.getInstance().executeSync(this::close);
                    }
                } catch (InvalidPathException e) {
                    Camerapture.LOGGER.error("tinyfd returned invalid path", e);
                }
            }
        }).start();
    }

    private boolean tryUpload(Path path) {
        boolean canTakePicture = this.client != null && this.client.player != null && CameraItem.canTakePicture(this.client.player);
        if (!canTakePicture) {
            return false;
        }

        PictureTaker.getInstance().tryUploadFile(path);
        return true;
    }
}