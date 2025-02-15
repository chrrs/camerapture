package me.chrr.camerapture.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.chrr.camerapture.Camerapture;

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

    public void load() {
        Path clientPath = getPath("client");
        Path serverPath = getPath("server");

        try {
            if (clientPath.toFile().isFile()) {
                config.client = GSON.fromJson(Files.readString(clientPath), Config.Client.class);
                config.client.upgrade();
            }

            if (serverPath.toFile().isFile()) {
                config.server = GSON.fromJson(Files.readString(serverPath), Config.Server.class);
                config.server.upgrade();
            }

            this.save();
        } catch (IOException e) {
            Camerapture.LOGGER.error("failed to load config", e);
        }
    }

    public void save() {
        try {
            if (Camerapture.PLATFORM.isClientSide()) {
                Files.writeString(getPath("client"), GSON.toJson(config.client));
            }

            Files.writeString(getPath("server"), GSON.toJson(config.server));
        } catch (IOException e) {
            Camerapture.LOGGER.error("failed to save config", e);
        }
    }

    private Path getPath(String environment) {
        return Camerapture.PLATFORM.getConfigFolder().resolve("camerapture." + environment + ".json");
    }
}