package me.chrr.camerapture;

import java.nio.file.Path;

public interface PlatformAdapter {
    Path getConfigFolder();

    Path getGameFolder();

    boolean isClientSide();

    boolean isModLoaded(String modId);

    default boolean canTakePicture() {
        return true;
    }
}
