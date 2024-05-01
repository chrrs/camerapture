package me.chrr.camerapture.screen;

import me.chrr.camerapture.picture.PictureTaker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.List;

public class UploadScreen extends Screen {
    public UploadScreen() {
        super(Text.of("Upload Picture"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xffffff);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void filesDragged(List<Path> paths) {
        for (Path path : paths) {
            if (PictureTaker.getInstance().tryUpload(path)) {
                this.close();
                return;
            }
        }
    }
}
