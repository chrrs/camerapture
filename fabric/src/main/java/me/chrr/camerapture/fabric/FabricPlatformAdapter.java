package me.chrr.camerapture.fabric;

import me.chrr.camerapture.PlatformAdapter;
import me.chrr.camerapture.fabric.event.ClientTakePictureCallback;
import me.chrr.camerapture.net.NetworkAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.ActionResult;

import java.nio.file.Path;

public class FabricPlatformAdapter implements PlatformAdapter {
    @Override
    public NetworkAdapter createNetworkAdapter() {
        return new FabricNetworkAdapter();
    }

    @Override
    public Path getConfigFolder() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Path getGameFolder() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public boolean canTakePicture() {
        ActionResult result = ClientTakePictureCallback.EVENT.invoker().takePicture();
        return result != ActionResult.FAIL;
    }
}
