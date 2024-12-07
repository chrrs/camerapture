package me.chrr.camerapture.net.clientbound;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record PictureErrorPacket(UUID uuid) {
    private static final Identifier ID = Camerapture.id("picture_error");
    public static final NetCodec<PictureErrorPacket> NET_CODEC = new NetCodec<>(ID,
            Uuids.CODEC.xmap(PictureErrorPacket::new, p -> p.uuid));
}