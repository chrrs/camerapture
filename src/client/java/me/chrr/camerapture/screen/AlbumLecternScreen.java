package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.AlbumItem;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.Text;

import java.util.List;

@SuppressWarnings("DataFlowIssue")
public class AlbumLecternScreen extends PictureScreen implements ScreenHandlerProvider<AlbumLecternScreenHandler>, ScreenHandlerListener {
    private final AlbumLecternScreenHandler handler;

    public AlbumLecternScreen(AlbumLecternScreenHandler handler) {
        super(List.of());
        this.handler = handler;
    }

    @Override
    protected void init() {
        super.init();

        this.handler.addListener(this);

        if (this.client.player.canModifyBlocks()) {
            addDrawableChild(ButtonWidget.builder(Text.translatable("lectern.take_book"), (button) ->
                            this.client.interactionManager.clickButton(this.handler.syncId, 0))
                    .dimensions(width / 2 - 40, height - PictureScreen.BORDER_THICKNESS + 2, 80, 16)
                    .build());
        }
    }

    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        if (stack.isOf(Camerapture.ALBUM)) {
            AlbumLecternScreen.this.setPictures(AlbumItem.getPictures(stack));
        }
    }

    @Override
    public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
    }

    @Override
    public AlbumLecternScreenHandler getScreenHandler() {
        return handler;
    }

    @Override
    public void close() {
        this.client.player.closeHandledScreen();
        super.close();
    }

    @Override
    public void removed() {
        this.handler.removeListener(this);
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
