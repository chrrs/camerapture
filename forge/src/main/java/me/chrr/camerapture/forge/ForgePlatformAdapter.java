package me.chrr.camerapture.forge;

import me.chrr.camerapture.PlatformAdapter;
import me.chrr.camerapture.net.NetworkAdapter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ForgePlatformAdapter implements PlatformAdapter {
    @Override
    public NetworkAdapter createNetworkAdapter() {
        return new ForgeNetworkAdapter();
    }

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
        return FMLLoader.getDist() == Dist.CLIENT;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
