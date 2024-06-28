package me.chrr.camerapture.net.serverbound;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record RequestPicturePacket(UUID uuid) {
    private static final Identifier ID = Camerapture.id("request_picture");
    public static final NetCodec<RequestPicturePacket> NET_CODEC = new NetCodec<>(ID,
            Uuids.CODEC.xmap(RequestPicturePacket::new, p -> p.uuid));
}
