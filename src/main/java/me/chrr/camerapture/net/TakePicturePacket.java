package me.chrr.camerapture.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record TakePicturePacket() implements FabricPacket {
    private static final Identifier ID = new Identifier("camerapture", "take_picture");
    public static final PacketType<TakePicturePacket> TYPE = PacketType.create(ID, TakePicturePacket::new);

    public TakePicturePacket(PacketByteBuf buf) {
        this();
    }

    @Override
    public void write(PacketByteBuf buf) {
    }

    @Override
    public PacketType<TakePicturePacket> getType() {
        return TYPE;
    }
}
