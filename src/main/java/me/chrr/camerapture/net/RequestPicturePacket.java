package me.chrr.camerapture.net;

import me.chrr.camerapture.Camerapture;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record RequestPicturePacket(UUID uuid) implements FabricPacket {
    private static final Identifier ID = Camerapture.id("request_picture");
    public static final PacketType<RequestPicturePacket> TYPE = PacketType.create(ID, RequestPicturePacket::new);

    public RequestPicturePacket(PacketByteBuf buf) {
        this(buf.readUuid());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(uuid);
    }

    @Override
    public PacketType<RequestPicturePacket> getType() {
        return TYPE;
    }
}
