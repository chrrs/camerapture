package me.chrr.camerapture.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().create();

    private final Config config = new Config();

    public Config getConfig() {
        return config;
    }

    public void load() throws IOException {
        Path clientPath = getPath("client");

        if (clientPath.toFile().isFile()) {
            config.client = GSON.fromJson(Files.readString(clientPath), Config.Client.class);
            config.client.upgrade();
        }

        save();
    }

    public void save() throws IOException {
        Path clientPath = getPath("client");
        Files.writeString(clientPath, GSON.toJson(config.client));
    }

    private Path getPath(String environment) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("camerapture." + environment + ".json");
    }

    public File getFile() {
        return getPath("client").toFile();
    }
}
