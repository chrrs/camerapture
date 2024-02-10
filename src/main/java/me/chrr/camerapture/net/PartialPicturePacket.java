package me.chrr.camerapture.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PartialPicturePacket(UUID uuid, byte[] bytes, int bytesLeft) implements FabricPacket {
    private static final Identifier ID = new Identifier("camerapture", "partial_picture");
    public static final PacketType<PartialPicturePacket> TYPE = PacketType.create(ID, PartialPicturePacket::new);

    public PartialPicturePacket(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readByteArray(), buf.readInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(uuid);
        buf.writeByteArray(bytes);
        buf.writeInt(bytesLeft);
    }

    @Override
    public PacketType<PartialPicturePacket> getType() {
        return TYPE;
    }
}
