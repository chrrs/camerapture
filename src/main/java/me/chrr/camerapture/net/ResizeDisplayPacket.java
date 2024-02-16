package me.chrr.camerapture.net;

import me.chrr.camerapture.Camerapture;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ResizeDisplayPacket(
        BlockPos pos, float offsetX, float offsetY, float width, float height
) implements FabricPacket {
    private static final Identifier ID = Camerapture.id("resize_display");
    public static final PacketType<ResizeDisplayPacket> TYPE = PacketType.create(ID, ResizeDisplayPacket::new);

    public ResizeDisplayPacket(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(offsetX);
        buf.writeFloat(offsetY);
        buf.writeFloat(width);
        buf.writeFloat(height);
    }

    @Override
    public PacketType<ResizeDisplayPacket> getType() {
        return TYPE;
    }
}