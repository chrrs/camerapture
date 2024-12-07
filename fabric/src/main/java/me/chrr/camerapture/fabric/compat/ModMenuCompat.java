package me.chrr.camerapture.fabric.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.chrr.camerapture.compat.ClothConfigScreenFactory;
import net.fabricmc.loader.api.FabricLoader;

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
