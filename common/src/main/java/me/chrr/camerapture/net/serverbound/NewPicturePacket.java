package me.chrr.camerapture.net.serverbound;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.ExtraCodecs;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.resources.Identifier;

public record NewPicturePacket() {
    private static final Identifier ID = Camerapture.id("new_picture");
    public static final NetCodec<NewPicturePacket> NET_CODEC = new NetCodec<>(ID, ExtraCodecs.unit(NewPicturePacket::new));
}