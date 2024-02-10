package me.chrr.camerapture.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record PictureErrorPacket(UUID uuid) implements FabricPacket {
    private static final Identifier ID = new Identifier("camerapture", "picture_error");
    public static final PacketType<PictureErrorPacket> TYPE = PacketType.create(ID, PictureErrorPacket::new);

    public PictureErrorPacket(PacketByteBuf buf) {
        this(buf.readUuid());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(uuid);
    }

    @Override
    public PacketType<PictureErrorPacket> getType() {
        return TYPE;
    }
}