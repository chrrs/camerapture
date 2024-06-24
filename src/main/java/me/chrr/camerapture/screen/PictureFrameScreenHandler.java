package me.chrr.camerapture.screen;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import org.jetbrains.annotations.Nullable;

public class PictureFrameScreenHandler extends ScreenHandler {
    @Nullable
    private final PictureFrameEntity entity;

    public PictureFrameScreenHandler(int syncId) {
        this(syncId, null, new ArrayPropertyDelegate(4));
    }

    public PictureFrameScreenHandler(int syncId, @Nullable PictureFrameEntity entity, PropertyDelegate propertyDelegate) {
        super(Camerapture.PICTURE_FRAME_SCREEN_HANDLER, syncId);

        checkDataCount(propertyDelegate, 4);
        this.addProperties(propertyDelegate);
        this.entity = entity;
    }

    @Override
    public void setProperty(int id, int value) {
        super.setProperty(id, value);
        this.sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
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
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
