package me.chrr.camerapture;

import me.chrr.camerapture.net.NetworkAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.nio.file.Path;

public interface PlatformAdapter {
    NetworkAdapter createNetworkAdapter();

    Path getConfigFolder();

    Path getGameFolder();

    boolean isClientSide();

    boolean isModLoaded(String modId);

    @Environment(EnvType.CLIENT)
    default boolean canTakePicture() {
        return true;
    }
}
