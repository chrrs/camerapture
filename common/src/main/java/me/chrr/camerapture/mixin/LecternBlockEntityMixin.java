package me.chrr.camerapture.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.gui.AlbumLecternMenu;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin {
    @Shadow
    @Final
    private Container bookAccess;

    @Shadow
    public abstract ItemStack getBook();

    @ModifyReturnValue(method = "hasBook", at = @At(value = "RETURN"))
    public boolean hasBook(boolean original) {
        return original || getBook().is(Camerapture.ALBUM);
    }

    /// Overwrite the book screen when the lectern contains an album.
    @Inject(method = "createMenu", at = @At(value = "HEAD"), cancellable = true)
    public void createMenu(int containerId, Inventory inventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> cir) {
        if (getBook().is(Camerapture.ALBUM)) {
            cir.setReturnValue(new AlbumLecternMenu(containerId, this.bookAccess));
            cir.cancel();
        }
    }
}