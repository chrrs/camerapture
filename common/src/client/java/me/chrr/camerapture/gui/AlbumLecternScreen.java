package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.item.AlbumItem;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.network.chat.Component;

import java.util.List;

@SuppressWarnings("DataFlowIssue")
public class AlbumLecternScreen extends PictureScreen implements MenuAccess<AlbumLecternMenu>, ContainerListener {
    private final AlbumLecternMenu menu;

    // We suppress the unused arguments as they are used in the client factory.
    public AlbumLecternScreen(AlbumLecternMenu menu, @SuppressWarnings("unused") Inventory playerInventory, @SuppressWarnings("unused") Component title) {
        super(List.of());
        this.menu = menu;
    }

    @Override
    protected void init() {
        super.init();

        this.menu.addSlotListener(this);

        if (this.minecraft.player.mayBuild()) {
            addRenderableWidget(Button.builder(Component.translatable("lectern.take_book"), (button) ->
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0))
                    .bounds(width / 2 - 40, height - PictureScreen.BORDER_THICKNESS + 2, 80, 16)
                    .build());
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slotId, ItemStack stack) {
        if (stack.is(Camerapture.ALBUM)) {
            AlbumLecternScreen.this.setPictures(AlbumItem.getPictures(stack));
        }
    }

    @Override
    public void dataChanged(AbstractContainerMenu menu, int property, int value) {
    }

    @Override
    public AlbumLecternMenu getMenu() {
        return menu;
    }

    @Override
    public void onClose() {
        this.minecraft.player.closeContainer();
        super.onClose();
    }

    @Override
    public void removed() {
        this.menu.removeSlotListener(this);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}