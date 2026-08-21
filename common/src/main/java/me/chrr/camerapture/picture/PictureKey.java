package me.chrr.camerapture.picture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

public record PictureKey(UUID id, PictureQuality quality) {
    public static final Codec<PictureKey> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.AUTHLIB_CODEC.fieldOf("id").forGetter(PictureKey::id),
            PictureQuality.CODEC.fieldOf("quality").forGetter(PictureKey::quality)
    ).apply(instance, PictureKey::new));
}
