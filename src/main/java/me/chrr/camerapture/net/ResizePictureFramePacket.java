package me.chrr.camerapture.net;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.entity.PictureFrameEntity;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record ResizePictureFramePacket(UUID uuid, PictureFrameEntity.ResizeDirection direction,
                                       boolean shrink) implements FabricPacket {
    private static final Identifier ID = Camerapture.id("resize_picture_frame");
    public static final PacketType<ResizePictureFramePacket> TYPE = PacketType.create(ID, ResizePictureFramePacket::new);

    public ResizePictureFramePacket(PacketByteBuf buf) {
        this(buf.readUuid(), buf.readEnumConstant(PictureFrameEntity.ResizeDirection.class), buf.readBoolean());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeUuid(uuid);
        buf.writeEnumConstant(direction);
        buf.writeBoolean(shrink);
    }

    @Override
    public PacketType<ResizePictureFramePacket> getType() {
        return TYPE;
    }
}
