package me.chrr.camerapture.neoforge;

import me.chrr.camerapture.PlatformAdapter;
import me.chrr.tapestry.gradle.annotation.Implementation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

@Implementation("camerapture:platform_adapter")
public class NeoForgePlatformAdapter implements PlatformAdapter {
    @Override
    public Path getConfigFolder() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Path getGameFolder() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public boolean isClientSide() {
        return FMLLoader.getCurrent().getDist() == Dist.CLIENT;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
