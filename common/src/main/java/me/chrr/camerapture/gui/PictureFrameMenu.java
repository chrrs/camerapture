package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class PictureFrameMenu extends AbstractContainerMenu {
    @Nullable
    private final PictureFrameEntity entity;

    public PictureFrameMenu(int containerId) {
        this(containerId, null, new SimpleContainerData(4));
    }

    public PictureFrameMenu(int containerId, @Nullable PictureFrameEntity entity, ContainerData propertyDelegate) {
        super(Camerapture.PICTURE_FRAME_SCREEN_HANDLER, containerId);

        checkContainerDataCount(propertyDelegate, 4);
        this.addDataSlots(propertyDelegate);
        this.entity = entity;
    }

    @Override
    public void setData(int id, int value) {
        super.setData(id, value);
        this.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.entity == null) {
            return false;
        }

        switch (id) {
            // 0-7: Resize (even = shrink, odd = grow)
            case 0, 1 -> {
                this.entity.resize(PictureFrameEntity.ResizeDirection.UP, id % 2 == 0);
                return true;
            }
            case 2, 3 -> {
                this.entity.resize(PictureFrameEntity.ResizeDirection.RIGHT, id % 2 == 0);
                return true;
            }
            case 4, 5 -> {
                this.entity.resize(PictureFrameEntity.ResizeDirection.DOWN, id % 2 == 0);
                return true;
            }
            case 6, 7 -> {
                this.entity.resize(PictureFrameEntity.ResizeDirection.LEFT, id % 2 == 0);
                return true;
            }

            // 8: Toggle glowing
            case 8 -> {
                this.entity.setPictureGlowing(!this.entity.isPictureGlowing());
                return true;
            }

            // 9: Toggle fixed
            case 9 -> {
                this.entity.setFixed(!this.entity.isFixed());
                return true;
            }

            default -> {
                return false;
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}