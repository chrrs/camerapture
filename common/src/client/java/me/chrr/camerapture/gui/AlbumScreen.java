package me.chrr.camerapture.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import me.chrr.camerapture.util.PictureDrawingUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.PageTurnWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AlbumScreen extends HandledScreen<AlbumScreenHandler> {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/edit_album.png");

    private int activePage = 0;
    private Text pageText = Text.empty();

    private PageTurnWidget previousButton;
    private PageTurnWidget nextButton;

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

        previousButton = addDrawableChild(new PageTurnWidget(x + 22, y + 121, false, button -> this.changePage(-1), true));
        nextButton = addDrawableChild(new PageTurnWidget(x + 234, y + 121, true, button -> this.changePage(1), true));
        updatePage();
    }

    private void changePage(int delta) {
        this.activePage = Math.min(Math.max(this.activePage + delta, 0), AlbumItem.PAGES);
        updatePage();
    }

    private void updatePage() {
        for (int i = 0; i < AlbumItem.SLOTS; i++) {
            int page = i / AlbumItem.ITEMS_PER_PAGE;
            ((PictureSlot) this.handler.slots.get(i)).setEnabled(page == this.activePage);
        }

        this.pageText = Text.translatable("book.pageIndicator", this.activePage + 1, AlbumItem.PAGES);
        this.previousButton.visible = this.activePage != 0;
        this.nextButton.visible = this.activePage != AlbumItem.PAGES - 1;
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

        // Draw the page number text
        int textWidth = this.textRenderer.getWidth(this.pageText);
        int pageX = this.backgroundWidth - this.titleX - textWidth;
        context.drawText(this.textRenderer, this.pageText, pageX, this.titleY, 0x000000, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
        int xOffset = mouseY - top > 148 ? 52 : 0;
        return mouseX < left + xOffset
                || mouseY < top
                || mouseX >= left + this.backgroundWidth - xOffset
                || mouseY >= top + this.backgroundHeight;
    }

    @Override
    protected void drawSlot(DrawContext context, Slot slot) {
        if (!(slot instanceof PictureSlot pictureSlot)) {
            super.drawSlot(context, slot);
            return;
        }

        RenderSystem.enableBlend();

        if (pictureSlot.hasStack()) {
            PictureItem.PictureData pictureData = PictureItem.getPictureData(slot.getStack());
            if (pictureData != null) {
                RemotePicture picture = ClientPictureStore.getInstance().ensureRemotePicture(pictureData.id());
                PictureDrawingUtil.drawPicture(context, textRenderer, picture,
                        slot.x, slot.y, pictureSlot.getWidth(), pictureSlot.getHeight());
            }
        } else {
            context.drawTexture(TEXTURE, slot.x - 1, slot.y - 1, 280, 0, pictureSlot.getWidth() + 2, pictureSlot.getHeight() + 2, 512, 512);
        }

        RenderSystem.disableBlend();
    }
}