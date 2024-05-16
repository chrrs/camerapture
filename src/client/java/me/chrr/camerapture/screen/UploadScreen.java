package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.gui.DrawContext;
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

public class UploadScreen extends InGameScreen {
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
    public void renderScreen(DrawContext context, int mouseX, int mouseY, float delta) {
        Text description = Text.translatable("text.camerapture.upload_picture.description");

        context.drawTexture(TEXTURE, width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2, 0, 0, backgroundWidth, backgroundHeight);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.height / 2 - textRenderer.fontHeight - 16, 0xffffff);

        int paper = CameraptureClient.paperInInventory();
        browseButton.visible = paper > 0;
        if (paper == 0) {
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
                        this.close();
                    }
                } catch (InvalidPathException ignored) {
                }
            }
        }).start();
    }

    private boolean tryUpload(Path path) {
        if (CameraptureClient.paperInInventory() == 0) {
            return false;
        }

        return PictureTaker.getInstance().tryUpload(path);
    }
}
