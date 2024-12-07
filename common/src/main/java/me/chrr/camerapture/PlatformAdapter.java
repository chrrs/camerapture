package me.chrr.camerapture;

import me.chrr.camerapture.net.NetworkAdapter;

import java.nio.file.Path;

public interface PlatformAdapter {
    NetworkAdapter createNetworkAdapter();

    Path getConfigFolder();

    Path getGameFolder();

    boolean isClientSide();

    boolean isModLoaded(String modId);
}
