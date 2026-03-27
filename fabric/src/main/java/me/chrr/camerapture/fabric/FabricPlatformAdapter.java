package me.chrr.camerapture.fabric;

import me.chrr.camerapture.PlatformAdapter;
import me.chrr.camerapture.fabric.event.ClientTakePictureCallback;
import me.chrr.tapestry.gradle.annotation.Implementation;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.InteractionResult;

import java.nio.file.Path;

@Implementation("camerapture:platform_adapter")
public class FabricPlatformAdapter implements PlatformAdapter {
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
    public boolean canTakePicture() {
        InteractionResult result = ClientTakePictureCallback.EVENT.invoker().takePicture();
        return result != InteractionResult.FAIL;
    }
}
