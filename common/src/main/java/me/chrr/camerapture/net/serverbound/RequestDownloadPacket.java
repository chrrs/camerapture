package me.chrr.camerapture.net.serverbound;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import me.chrr.camerapture.picture.PictureQuality;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RequestDownloadPacket(UUID uuid, PictureQuality quality) {
    private static final Identifier ID = Camerapture.id("request_download");
    public static final NetCodec<RequestDownloadPacket> NET_CODEC = new NetCodec<>(ID,
            RecordCodecBuilder.create(instance -> instance.group(
                    UUIDUtil.AUTHLIB_CODEC.fieldOf("uuid").forGetter(RequestDownloadPacket::uuid),
                    PictureQuality.CODEC.fieldOf("quality").forGetter(RequestDownloadPacket::quality)
            ).apply(instance, RequestDownloadPacket::new)));
}