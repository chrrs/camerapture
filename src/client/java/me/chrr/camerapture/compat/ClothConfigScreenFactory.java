package me.chrr.camerapture.compat;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class ClothConfigScreenFactory {
    private static final Logger LOGGER = LogManager.getLogger();

    private ClothConfigScreenFactory() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.camerapture.title"));

        builder.setSavingRunnable(() -> {
            try {
                Camerapture.getConfigManager().save();
            } catch (IOException e) {
                LOGGER.error("could not save config", e);
            }
        });

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        builder.getOrCreateCategory(Text.empty())
                .addEntry(buildClientCategory(entryBuilder))
                .addEntry(buildServerCategory(entryBuilder));

        return builder.build();
    }

    private static SubCategoryListEntry buildClientCategory(ConfigEntryBuilder builder) {
        Config config = Camerapture.getConfigManager().getConfig();

        SubCategoryBuilder client = builder
                .startSubCategory(Text.translatable("config.camerapture.category.client"))
                .setExpanded(true);

        client.add(builder.startBooleanToggle(
                        Text.translatable("config.camerapture.option.cache_pictures"),
                        config.client.cachePictures
                )
                .setSaveConsumer((value) -> config.client.cachePictures = value)
                .build());

        return client.build();
    }

    private static SubCategoryListEntry buildServerCategory(ConfigEntryBuilder builder) {
        SubCategoryBuilder server = builder
                .startSubCategory(Text.translatable("config.camerapture.category.server"))
                .setExpanded(true);

        return server.build();
    }

    public static boolean isClothConfigInstalled() {
        return FabricLoader.getInstance().isModLoaded("cloth-config2");
    }
}