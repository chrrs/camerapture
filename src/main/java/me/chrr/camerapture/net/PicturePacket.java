package me.chrr.camerapture.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PicturePacket(UUID uuid, byte[] bytes) implements FabricPacket {
    private static final Identifier ID = new Identifier("camerapture", "picture");
    public static final PacketType<PicturePacket> TYPE = PacketType.create(ID, PicturePacket::new);

    public PicturePacket(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readByteArray());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(uuid);
        buf.writeByteArray(bytes);
    }

    @Override
    public PacketType<PicturePacket> getType() {
        return TYPE;
    }
}
