package me.chrr.camerapture.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SyncedConfig(
        boolean allowUploading,
        int maxImageBytes, int maxImageResolution) {
    public static final Codec<SyncedConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("allowUploading").forGetter(p -> p.allowUploading),
            Codec.INT.fieldOf("maxImageBytes").forGetter(p -> p.maxImageBytes),
            Codec.INT.fieldOf("maxImageResolution").forGetter(p -> p.maxImageResolution)
    ).apply(instance, SyncedConfig::new));

    public static SyncedConfig fromServerConfig(Config.Server serverConfig) {
        return new SyncedConfig(serverConfig.allowUploading, serverConfig.maxImageBytes, serverConfig.maxImageResolution);
    }
}
