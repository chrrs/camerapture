package me.chrr.camerapture.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.chrr.camerapture.compat.ClothConfigScreenFactory;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import net.fabricmc.loader.api.FabricLoader;

@FabricEntrypoint("modmenu")
public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {
            return ClothConfigScreenFactory::create;
        } else {
            return null;
        }
    }
}
