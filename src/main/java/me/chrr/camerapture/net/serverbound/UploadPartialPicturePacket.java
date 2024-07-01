package me.chrr.camerapture.net.serverbound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.ExtraCodecs;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record UploadPartialPicturePacket(UUID uuid, byte[] bytes, int bytesLeft) {
    private static final Identifier ID = Camerapture.id("upload_partial_picture");
    public static final NetCodec<UploadPartialPicturePacket> NET_CODEC = new NetCodec<>(ID,
            RecordCodecBuilder.create(instance -> instance.group(
                    Uuids.CODEC.fieldOf("uuid").forGetter(p -> p.uuid),
                    ExtraCodecs.BYTE_ARRAY.fieldOf("bytes").forGetter(p -> p.bytes),
                    Codec.INT.fieldOf("bytesLeft").forGetter(p -> p.bytesLeft)
            ).apply(instance, UploadPartialPicturePacket::new)));
}
