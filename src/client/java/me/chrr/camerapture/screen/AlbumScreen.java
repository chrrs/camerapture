package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AlbumScreen extends HandledScreen<AlbumScreenHandler> {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/edit_album.png");

    public AlbumScreen(AlbumScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);

        this.backgroundWidth = 280;
        this.backgroundHeight = 237;

        this.playerInventoryTitleX = 60;
        this.playerInventoryTitleY = this.backgroundHeight - 94;

        this.titleX = 19;
        this.titleY = 15;
    }

    @Override
    protected void init() {
        super.init();

        addDrawableChild(new PageTurnWidget(x + 22, y + 121, false, button -> {}, true));
        addDrawableChild(new PageTurnWidget(x + 234, y + 121, true, button -> {}, true));
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 512, 512);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // We're overriding this method to make the inventory title black.
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x000000, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        int xOffset = mouseY > 148 ? 52 : 0;
        return mouseX < left + xOffset
                || mouseY < top
                || mouseX >= left + this.backgroundWidth - xOffset
                || mouseY >= top + this.backgroundHeight;
    }
}
