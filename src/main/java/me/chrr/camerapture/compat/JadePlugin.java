package me.chrr.camerapture.compat;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.item.PictureItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEntityComponent(PictureFrameComponentProvider.INSTANCE, PictureFrameEntity.class);
    }

    private enum PictureFrameComponentProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            PictureFrameEntity entity = (PictureFrameEntity) accessor.getEntity();
            ItemStack itemStack = entity.getItemStack();
            if (itemStack == null) {
                return;
            }

            PictureItem.getTooltip(tooltip::add, itemStack);
        }

        @Override
        public Identifier getUid() {
            return Camerapture.id("jade_compat/picture_frame");
        }
    }
}
