package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/// The server-side picture store. It is actually a bit of a misnomer, as this
/// picture store can store any byte files, and does not do any image parsing.
///
/// Pictures put in the store will be stored on the disk in the world folder.
/// The store also manages picture UUID's.
public class ServerPictureStore {
    private static final int CACHE_SIZE = 250;

    private static final ServerPictureStore INSTANCE = new ServerPictureStore();

    private final Set<UUID> reservedIds = new HashSet<>();
    private final Map<UUID, StoredPicture> pictureCache = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, StoredPicture> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /// Use {@link #getInstance()} instead of creating a new one.
    private ServerPictureStore() {
    }

    public UUID reserveId() {
        UUID id = UUID.randomUUID();
        reservedIds.add(id);
        return id;
    }

    public boolean unreserveId(UUID id) {
        return reservedIds.remove(id);
    }

    public boolean isReserved(UUID id) {
        return reservedIds.contains(id);
    }

    public void put(MinecraftServer server, UUID id, StoredPicture picture) throws IOException {
        if (!unreserveId(id)) {
            throw new IOException("UUID not reserved");
        }

        int maxImageBytes = Camerapture.CONFIG_MANAGER.getConfig().server.maxImageBytes;
        if (picture.bytes().length > maxImageBytes) {
            throw new IOException("image larger than " + maxImageBytes + " bytes");
        }

        pictureCache.put(id, picture);

        Path path = getFilePath(server, id);
        Files.createDirectories(path.getParent());
        Files.write(path, picture.bytes());
    }

    @Nullable
    public StoredPicture get(MinecraftServer server, UUID id) throws IOException {
        if (pictureCache.containsKey(id)) {
            return pictureCache.get(id);
        }

        Path path = getFilePath(server, id);
        if (!Files.exists(path)) {
            return null;
        }

        StoredPicture picture = new StoredPicture(Files.readAllBytes(path));
        pictureCache.put(id, picture);
        return picture;
    }

    private Path getFilePath(MinecraftServer server, UUID uuid) {
        Path dataFolder = server.getSavePath(WorldSavePath.ROOT).resolve("camerapture");
        return dataFolder.resolve(uuid + ".webp");
    }

    public static ServerPictureStore getInstance() {
        return INSTANCE;
    }
}