package me.chrr.camerapture.net.clientbound;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record RequestUploadPacket(UUID uuid) {
    private static final Identifier ID = Camerapture.id("request_upload");
    public static final NetCodec<RequestUploadPacket> NET_CODEC = new NetCodec<>(ID,
            Uuids.CODEC.xmap(RequestUploadPacket::new, p -> p.uuid));
}
