package me.chrr.camerapture.net.clientbound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.ExtraCodecs;
import me.chrr.camerapture.net.NetCodec;
import me.chrr.camerapture.picture.PictureQuality;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record DownloadPartialPicturePacket(UUID uuid, PictureQuality quality, byte[] bytes, int bytesLeft) {
    private static final Identifier ID = Camerapture.id("download_partial_picture");
    public static final NetCodec<DownloadPartialPicturePacket> NET_CODEC = new NetCodec<>(ID,
            RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.AUTHLIB_CODEC.fieldOf("uuid").forGetter(DownloadPartialPicturePacket::uuid),
                    PictureQuality.CODEC.fieldOf("quality").forGetter(DownloadPartialPicturePacket::quality),
                    ExtraCodecs.BYTE_ARRAY.fieldOf("bytes").forGetter(DownloadPartialPicturePacket::bytes),
                    Codec.INT.fieldOf("bytesLeft").forGetter(DownloadPartialPicturePacket::bytesLeft)
            ).apply(instance, DownloadPartialPicturePacket::new)));
}