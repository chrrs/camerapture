package me.chrr.camerapture.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.screen.AlbumLecternScreenHandler;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
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
    private Inventory inventory;

    @Shadow
    public abstract ItemStack getBook();

    @ModifyReturnValue(method = "hasBook", at = @At(value = "RETURN"))
    public boolean hasBook(boolean original) {
        return original || getBook().isOf(Camerapture.ALBUM);
    }

    // Overwrite the book screen when the lectern contains an album.
    @Inject(method = "createMenu", at = @At(value = "HEAD"), cancellable = true)
    public void createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity, CallbackInfoReturnable<ScreenHandler> cir) {
        if (getBook().isOf(Camerapture.ALBUM)) {
            cir.setReturnValue(new AlbumLecternScreenHandler(i, this.inventory));
            cir.cancel();
        }
    }
}
