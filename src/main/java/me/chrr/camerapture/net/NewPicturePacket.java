package me.chrr.camerapture.net;

import me.chrr.camerapture.Camerapture;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public record NewPicturePacket() implements FabricPacket {
    private static final Identifier ID = Camerapture.id("new_picture");
    public static final PacketType<NewPicturePacket> TYPE = PacketType.create(ID, NewPicturePacket::new);

    public NewPicturePacket(PacketByteBuf buf) {
        this();
    }

    @Override
    public void write(PacketByteBuf buf) {
    }

    @Override
    public PacketType<NewPicturePacket> getType() {
        return TYPE;
    }
}
