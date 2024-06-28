package me.chrr.camerapture.net.serverbound;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record RequestDownloadPacket(UUID uuid) {
    private static final Identifier ID = Camerapture.id("request_download");
    public static final NetCodec<RequestDownloadPacket> NET_CODEC = new NetCodec<>(ID,
            Uuids.CODEC.xmap(RequestDownloadPacket::new, p -> p.uuid));
}
