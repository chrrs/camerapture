package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class DisplayScreen extends HandledScreen<DisplayScreenHandler> {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/container/display.png");

    public DisplayScreen(DisplayScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, Text.empty());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2, 0, 0, backgroundWidth, backgroundHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
