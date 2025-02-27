package me.chrr.camerapture.compat;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.config.Config;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.SubCategoryListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public enum ClothConfigScreenFactory {
    ;

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.camerapture.title"));

        builder.setSavingRunnable(Camerapture.CONFIG_MANAGER::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        builder.getOrCreateCategory(Text.empty())
                .addEntry(buildClientCategory(entryBuilder))
                .addEntry(buildServerCategory(entryBuilder));

        return builder.build();
    }

    private static SubCategoryListEntry buildClientCategory(ConfigEntryBuilder builder) {
        Config config = Camerapture.CONFIG_MANAGER.getConfig();

        SubCategoryBuilder client = builder
                .startSubCategory(Text.translatable("config.camerapture.category.client"))
                .setExpanded(true);

        client.add(builder.startBooleanToggle(
                        Text.translatable("config.camerapture.option.cache_pictures"),
                        config.client.cachePictures
                )
                .setDefaultValue(false)
                .setSaveConsumer((value) -> config.client.cachePictures = value)
                .build());

        client.add(builder.startBooleanToggle(
                        Text.translatable("config.camerapture.option.save_screenshot"),
                        config.client.saveScreenshot
                )
                .setDefaultValue(false)
                .setSaveConsumer((value) -> config.client.saveScreenshot = value)
                .build());

        client.add(builder.startBooleanToggle(
                        Text.translatable("config.camerapture.option.simple_camera_hud"),
                        config.client.simpleCameraHud
                )
                .setDefaultValue(false)
                .setSaveConsumer((value) -> config.client.simpleCameraHud = value)
                .build());

        client.add(builder.startIntSlider(
                        Text.translatable("config.camerapture.option.zoom_mouse_sensitivity"),
                        (int) (config.client.zoomMouseSensitivity * 100f),
                        10, 100
                )
                .setDefaultValue(50)
                .setTextGetter((value) -> Text.of(value + "%"))
                .setSaveConsumer((value) -> config.client.zoomMouseSensitivity = (float) value / 100f)
                .build());

        return client.build();
    }

    private static SubCategoryListEntry buildServerCategory(ConfigEntryBuilder builder) {
        Config config = Camerapture.CONFIG_MANAGER.getConfig();

        SubCategoryBuilder server = builder
                .startSubCategory(Text.translatable("config.camerapture.category.server"))
                .setExpanded(true);

        server.add(builder.startIntField(
                        Text.translatable("config.camerapture.option.max_image_bytes"),
                        config.server.maxImageBytes
                )
                .setDefaultValue(500_000)
                .setMin(100_000)
                .setTooltip(Text.translatable("config.camerapture.set_by_server"))
                .setSaveConsumer((value) -> config.server.maxImageBytes = value)
                .build());

        server.add(builder.startIntField(
                        Text.translatable("config.camerapture.option.max_image_resolution"),
                        config.server.maxImageResolution
                )
                .setDefaultValue(1920)
                .setMin(1)
                .setTooltip(Text.translatable("config.camerapture.set_by_server"))
                .setSaveConsumer((value) -> config.server.maxImageResolution = value)
                .build());

        server.add(builder.startBooleanToggle(
                        Text.translatable("config.camerapture.option.allow_uploading"),
                        config.server.allowUploading
                )
                .setDefaultValue(true)
                .setTooltip(Text.translatable("config.camerapture.set_by_server"))
                .setSaveConsumer((value) -> config.server.allowUploading = value)
                .build());

        server.add(builder.startIntField(
                        Text.translatable("config.camerapture.option.ms_per_picture"),
                        config.server.msPerPicture
                )
                .setDefaultValue(20)
                .setMin(1)
                .setSaveConsumer((value) -> config.server.msPerPicture = value)
                .build());

        server.add(builder.startBooleanToggle(
                        Text.translatable("config.camerapture.option.can_rotate_pictures"),
                        config.server.canRotatePictures
                )
                .setDefaultValue(true)
                .setSaveConsumer((value) -> config.server.canRotatePictures = value)
                .build());

        server.add(builder.startBooleanToggle(
                        Text.translatable("config.camerapture.option.check_frame_position"),
                        config.server.checkFramePosition
                )
                .setDefaultValue(false)
                .setSaveConsumer((value) -> config.server.checkFramePosition = value)
                .build());

        return server.build();
    }
}