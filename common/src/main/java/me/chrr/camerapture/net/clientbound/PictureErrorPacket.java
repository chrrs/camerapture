package me.chrr.camerapture.net.clientbound;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import me.chrr.camerapture.picture.PictureQuality;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record PictureErrorPacket(UUID uuid, PictureQuality quality) {
    private static final Identifier ID = Camerapture.id("picture_error");
    public static final NetCodec<PictureErrorPacket> NET_CODEC = new NetCodec<>(ID,
            RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.AUTHLIB_CODEC.fieldOf("uuid").forGetter(PictureErrorPacket::uuid),
                    PictureQuality.CODEC.fieldOf("quality").forGetter(PictureErrorPacket::quality)
            ).apply(instance, PictureErrorPacket::new)));
}