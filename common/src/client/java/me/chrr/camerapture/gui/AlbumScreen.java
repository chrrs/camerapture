package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.RemotePicture;
import me.chrr.camerapture.util.PictureDrawingUtil;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;

public class AlbumScreen extends AbstractContainerScreen<AlbumMenu> {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/edit_album.png");

    private int activePage = 0;
    private Component pageText = Component.empty();

    private PageButton previousButton;
    private PageButton nextButton;

    public AlbumScreen(AlbumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.imageWidth = 280;
        this.imageHeight = 237;

        this.inventoryLabelX = 60;
        this.inventoryLabelY = this.imageHeight - 94;

        this.titleLabelX = 19;
        this.titleLabelY = 15;
    }

    @Override
    protected void init() {
        super.init();

        previousButton = addRenderableWidget(new PageButton(leftPos + 22, topPos + 121, false, button -> this.changePage(-1), true));
        nextButton = addRenderableWidget(new PageButton(leftPos + 234, topPos + 121, true, button -> this.changePage(1), true));
        updatePage();
    }

    private void changePage(int delta) {
        this.activePage = Math.min(Math.max(this.activePage + delta, 0), AlbumItem.PAGES);
        updatePage();
    }

    private void updatePage() {
        for (int i = 0; i < AlbumItem.SLOTS; i++) {
            int page = i / AlbumItem.ITEMS_PER_PAGE;
            ((PictureSlot) this.menu.slots.get(i)).setEnabled(page == this.activePage);
        }

        this.pageText = Component.translatable("book.pageIndicator", this.activePage + 1, AlbumItem.PAGES);
        this.previousButton.visible = this.activePage != 0;
        this.nextButton.visible = this.activePage != AlbumItem.PAGES - 1;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 512, 512);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // We're overriding this method to make the inventory title black.
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, CommonColors.BLACK, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, CommonColors.DARK_GRAY, false);

        // Draw the page number text
        int textWidth = this.font.width(this.pageText);
        int pageX = this.imageWidth - this.titleLabelX - textWidth;
        graphics.drawString(this.font, this.pageText, pageX, this.titleLabelY, CommonColors.BLACK, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        int xOffset = mouseY - top > 148 ? 52 : 0;
        return mouseX < left + xOffset
                || mouseY < top
                || mouseX >= left + this.imageWidth - xOffset
                || mouseY >= top + this.imageHeight;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot, int mouseX, int mouseY) {
        if (!(slot instanceof PictureSlot pictureSlot)) {
            super.renderSlot(graphics, slot, mouseX, mouseY);
            return;
        }

        if (pictureSlot.hasItem()) {
            PictureItem.PictureData pictureData = PictureItem.getPictureData(slot.getItem());
            if (pictureData != null) {
                RemotePicture picture = ClientPictureStore.getInstance().ensureRemotePicture(pictureData.id());
                PictureDrawingUtil.drawPicture(graphics, font, picture,
                        slot.x, slot.y, pictureSlot.getWidth(), pictureSlot.getHeight());
            }
        } else {
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, slot.x - 1, slot.y - 1, 280, 0, pictureSlot.getWidth() + 2, pictureSlot.getHeight() + 2, 512, 512);
        }
    }
}