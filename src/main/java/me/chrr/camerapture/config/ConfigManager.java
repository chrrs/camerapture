package me.chrr.camerapture.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

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
        Path serverPath = getPath("server");

        if (clientPath.toFile().isFile()) {
            config.client = GSON.fromJson(Files.readString(clientPath), Config.Client.class);
            config.client.upgrade();
        }

        if (serverPath.toFile().isFile()) {
            config.server = GSON.fromJson(Files.readString(serverPath), Config.Server.class);
            config.server.upgrade();
        }

        this.save();
    }

    public void save() throws IOException {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            Files.writeString(getPath("client"), GSON.toJson(config.client));
        }

        Files.writeString(getPath("server"), GSON.toJson(config.server));
    }

    private Path getPath(String environment) {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("camerapture." + environment + ".json");
    }
}
