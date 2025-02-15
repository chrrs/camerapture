package me.chrr.camerapture.net.clientbound;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import net.minecraft.util.Identifier;

public record SyncConfigPacket(int maxImageBytes, int maxImageResolution, boolean allowUploading) {
    private static final Identifier ID = Camerapture.id("sync_config");
    public static final NetCodec<SyncConfigPacket> NET_CODEC = new NetCodec<>(ID,
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("maxImageBytes").forGetter(p -> p.maxImageBytes),
                    Codec.INT.fieldOf("maxImageResolution").forGetter(p -> p.maxImageResolution),
                    Codec.BOOL.fieldOf("allowUploading").forGetter(p -> p.allowUploading)
            ).apply(instance, SyncConfigPacket::new)));
}