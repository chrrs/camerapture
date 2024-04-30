package me.chrr.camerapture.net;

import me.chrr.camerapture.Camerapture;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record EditPictureFramePacket(UUID uuid, boolean glowing, boolean fixed) implements FabricPacket {
    private static final Identifier ID = Camerapture.id("edit_picture_frame");
    public static final PacketType<EditPictureFramePacket> TYPE = PacketType.create(ID, EditPictureFramePacket::new);

    public EditPictureFramePacket(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readBoolean(), buf.readBoolean());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(uuid);
        buf.writeBoolean(glowing);
        buf.writeBoolean(fixed);
    }

    @Override
    public PacketType<EditPictureFramePacket> getType() {
        return TYPE;
    }
}
