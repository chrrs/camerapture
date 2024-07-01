package me.chrr.camerapture.net.serverbound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;

public record SyncConfigPacket(int maxImageBytes, int maxImageResolution) {
    private static final Identifier ID = Camerapture.id("sync_config");
    public static final NetCodec<SyncConfigPacket> NET_CODEC = new NetCodec<>(ID,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("maxImageBytes").forGetter(p -> p.maxImageBytes),
                    Codec.INT.fieldOf("maxImageResolution").forGetter(p -> p.maxImageResolution)
            ).apply(instance, SyncConfigPacket::new)));
}
