package me.chrr.camerapture.net;

import me.chrr.camerapture.Camerapture;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record ConfigPacket(int maxImageBytes, int maxImageResolution) implements FabricPacket {
    private static final Identifier ID = Camerapture.id("config");
    public static final PacketType<ConfigPacket> TYPE = PacketType.create(ID, ConfigPacket::new);

    public ConfigPacket(PacketByteBuf buf) {
        this(buf.readInt(), buf.readInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(maxImageBytes);
        buf.writeInt(maxImageResolution);
    }

    @Override
    public PacketType<ConfigPacket> getType() {
        return TYPE;
    }
}
