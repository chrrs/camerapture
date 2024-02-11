package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ServerImageStore {
    private static final int CACHE_SIZE = 200;

    private static final ServerImageStore INSTANCE = new ServerImageStore();

    private final List<UUID> reservedUuids = new ArrayList<>();
    private final Map<UUID, Image> imageCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Image> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private ServerImageStore() {
    }

    public UUID reserveUuid() {
        UUID uuid = UUID.randomUUID();
        reservedUuids.add(uuid);
        return uuid;
    }

    public void put(MinecraftServer server, UUID uuid, Image image) throws IOException {
        if (!reservedUuids.remove(uuid)) {
            throw new IOException("UUID not reserved");
        }

        if (image.bytes().length > Camerapture.MAX_IMAGE_BYTES) {
            throw new IOException("image larger than " + Camerapture.MAX_IMAGE_BYTES + " bytes");
        }

        imageCache.put(uuid, image);

        Path path = getFilePath(server, uuid);
        Files.createDirectories(path.getParent());
        Files.write(path, image.bytes);
    }

    public Image get(MinecraftServer server, UUID uuid) throws IOException {
        if (imageCache.containsKey(uuid)) {
            return imageCache.get(uuid);
        }

        Path path = getFilePath(server, uuid);

        if (!Files.exists(path)) {
            return null;
        }

        Image image = new Image(Files.readAllBytes(path));
        imageCache.put(uuid, image);
        return image;
    }

    private Path getFilePath(MinecraftServer server, UUID uuid) {
        Path dataFolder = server.getSavePath(WorldSavePath.ROOT).resolve("camerapture");
        return dataFolder.resolve(uuid + ".jpg");
    }

    public static ServerImageStore getInstance() {
        return INSTANCE;
    }

    public record Image(byte[] bytes) {
    }
}
