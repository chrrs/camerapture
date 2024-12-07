package me.chrr.camerapture.net.serverbound;

import com.mojang.serialization.Codec;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;

public record NewPicturePacket() {
    private static final Identifier ID = Camerapture.id("new_picture");
    public static final NetCodec<NewPicturePacket> NET_CODEC = new NetCodec<>(ID, Codec.unit(NewPicturePacket::new));
}