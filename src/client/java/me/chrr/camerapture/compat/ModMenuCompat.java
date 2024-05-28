package me.chrr.camerapture.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (ClothConfigScreenFactory.isClothConfigInstalled()) {
            return ClothConfigScreenFactory::create;
        } else {
            return null;
        }
    }

}
