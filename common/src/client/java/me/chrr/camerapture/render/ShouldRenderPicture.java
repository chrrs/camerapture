package me.chrr.camerapture.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.render.item.property.bool.BooleanProperty;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record ShouldRenderPicture() implements BooleanProperty {
    public static final MapCodec<ShouldRenderPicture> MAP_CODEC = MapCodec.unit(ShouldRenderPicture::new);

    @Override
    public boolean test(ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity, int seed, ItemDisplayContext displayContext) {
        return PictureItemRenderer.canRender(stack);
    }

    @Override
    public MapCodec<? extends BooleanProperty> getCodec() {
        return MAP_CODEC;
    }
}
