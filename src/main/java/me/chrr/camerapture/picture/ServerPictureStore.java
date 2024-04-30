package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ServerPictureStore {
    private static final int CACHE_SIZE = 250;

    private static final ServerPictureStore INSTANCE = new ServerPictureStore();

    private final Set<UUID> reservedUuids = new HashSet<>();
    private final Map<UUID, Picture> imageCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Picture> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    private ServerPictureStore() {
    }

    public UUID reserveUuid() {
        UUID uuid = UUID.randomUUID();
        reservedUuids.add(uuid);
        return uuid;
    }

    public boolean unreserveUuid(UUID uuid) {
        return reservedUuids.remove(uuid);
    }

    public boolean isReserved(UUID uuid) {
        return reservedUuids.contains(uuid);
    }

    public void put(MinecraftServer server, UUID uuid, Picture picture) throws IOException {
        if (!unreserveUuid(uuid)) {
            throw new IOException("UUID not reserved");
        }

        if (picture.bytes().length > Camerapture.MAX_IMAGE_BYTES) {
            throw new IOException("image larger than " + Camerapture.MAX_IMAGE_BYTES + " bytes");
        }

        imageCache.put(uuid, picture);

        Path path = getFilePath(server, uuid);
        Files.createDirectories(path.getParent());
        Files.write(path, picture.bytes);
    }

    @Nullable
    public Picture get(MinecraftServer server, UUID uuid) throws IOException {
        if (imageCache.containsKey(uuid)) {
            return imageCache.get(uuid);
        }

        Path path = getFilePath(server, uuid);

        if (!Files.exists(path)) {
            return null;
        }

        Picture picture = new Picture(Files.readAllBytes(path));
        imageCache.put(uuid, picture);
        return picture;
    }

    private Path getFilePath(MinecraftServer server, UUID uuid) {
        Path dataFolder = server.getSavePath(WorldSavePath.ROOT).resolve("camerapture");
        return dataFolder.resolve(uuid + ".webp");
    }

    public static ServerPictureStore getInstance() {
        return INSTANCE;
    }

    public record Picture(byte[] bytes) {
    }
}
