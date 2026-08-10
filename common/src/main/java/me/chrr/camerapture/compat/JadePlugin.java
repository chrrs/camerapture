package me.chrr.camerapture.compat;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.block.PictureFrameBlock;
import me.chrr.camerapture.block.PictureFrameBlockEntity;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
@FabricEntrypoint("jade")
public class JadePlugin implements IWailaPlugin {
    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PictureFrameBlockComponentProvider.INSTANCE, PictureFrameBlock.class);
    }

    private enum PictureFrameBlockComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockEntity() instanceof PictureFrameBlockEntity blockEntity) {
                ItemStack itemStack = blockEntity.getItemStack();
                if (itemStack == null || itemStack.isEmpty()) {
                    return;
                }
                PictureItem.getTooltip(tooltip::add, itemStack);
            }
        }

        @Override
        public Identifier getUid() {
            return Camerapture.id("picture_frame_block");
        }
    }
}