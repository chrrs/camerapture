package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/// The server-side picture store. It is actually a bit of a misnomer, as this
/// picture store can store any byte files, and does not do any image parsing.
///
/// Pictures put in the store will be stored on the disk in the world folder.
/// The store also manages picture UUID's.
public class ServerPictureStore {
    /// The cache is bounded by bytes rather than by entry count. Pictures vary hugely in size, so a
    /// fixed entry count either wastes memory on small ones or thrashes on large ones — and a world
    /// with a few thousand posters would hold only a small fraction of them, sending almost every
    /// request to the disk. At the 500KB default picture limit this holds at least ~500 pictures,
    /// and many more in practice since most are far smaller.
    private static final long MAX_CACHE_BYTES = 256L * 1024L * 1024L;

    private static final ServerPictureStore INSTANCE = new ServerPictureStore();

    private final Set<UUID> reservedIds = ConcurrentHashMap.newKeySet();

    /// Access-ordered LRU guarded by its own monitor, with `cacheBytes` tracking its total weight.
    private final LinkedHashMap<UUID, StoredPicture> pictureCache = new LinkedHashMap<>(256, 0.75f, true);
    private long cacheBytes = 0L;

    /// Per-picture load locks, so concurrent misses for the same picture collapse into one disk read.
    private final Map<UUID, Object> loadLocks = new ConcurrentHashMap<>();

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

        // Resolution is enforced here as well as by the uploading client, which only ever received the
        // limit as a suggestion. A byte limit alone doesn't bound the decoded size — a highly
        // compressible image at WebP's 16383x16383 maximum fits easily inside the byte limit but costs
        // every client that later renders it over a gigabyte of heap.
        int maxImageResolution = Camerapture.CONFIG_MANAGER.getConfig().server.maxImageResolution;
        WebPHeader.Size size = WebPHeader.read(picture.bytes());
        if (size == null) {
            throw new IOException("image is not a readable WebP");
        }

        if (size.width() > maxImageResolution || size.height() > maxImageResolution) {
            throw new IOException("image is " + size.width() + "x" + size.height()
                    + ", larger than " + maxImageResolution + " in at least one dimension");
        }

        cache(id, picture);

        Path path = getFilePath(server, id);
        Files.createDirectories(path.getParent());
        Files.write(path, picture.bytes());
    }

    @Nullable
    public StoredPicture get(MinecraftServer server, UUID id) throws IOException {
        StoredPicture cached = getCached(id);
        if (cached != null) {
            return cached;
        }

        // Collapse concurrent misses for the same picture. When a crowd walks into the same area they
        // all ask for the same posters at once; without this, every one of those requests would do its
        // own disk read and allocate its own copy of the bytes.
        Object loadLock = loadLocks.computeIfAbsent(id, key -> new Object());
        try {
            synchronized (loadLock) {
                cached = getCached(id);
                if (cached != null) {
                    return cached;
                }

                Path path = getFilePath(server, id);
                if (!Files.exists(path)) {
                    return null;
                }

                StoredPicture picture = new StoredPicture(Files.readAllBytes(path));
                cache(id, picture);
                return picture;
            }
        } finally {
            loadLocks.remove(id, loadLock);
        }
    }

    @Nullable
    private StoredPicture getCached(UUID id) {
        synchronized (pictureCache) {
            // Has to be a single lookup: containsKey/get is not atomic, so a concurrent eviction between
            // the two would report the picture as missing. It also keeps the LRU honest — the cache is
            // access-ordered, and containsKey does not count as an access.
            return pictureCache.get(id);
        }
    }

    /// Add a picture to the cache, evicting least-recently-used entries until it fits.
    private void cache(UUID id, StoredPicture picture) {
        synchronized (pictureCache) {
            StoredPicture previous = pictureCache.put(id, picture);
            if (previous != null) {
                cacheBytes -= previous.bytes().length;
            }
            cacheBytes += picture.bytes().length;

            // Access order puts the least recently used entry first. The entry we just inserted is the
            // most recent, so it's last and only evicted if it alone is over the cap.
            Iterator<Map.Entry<UUID, StoredPicture>> iterator = pictureCache.entrySet().iterator();
            while (cacheBytes > MAX_CACHE_BYTES && pictureCache.size() > 1 && iterator.hasNext()) {
                Map.Entry<UUID, StoredPicture> eldest = iterator.next();
                cacheBytes -= eldest.getValue().bytes().length;
                iterator.remove();
            }
        }
    }

    private Path getFilePath(MinecraftServer server, UUID uuid) {
        Path dataFolder = server.getWorldPath(LevelResource.ROOT).resolve("camerapture");
        return dataFolder.resolve(uuid + ".webp");
    }

    public static ServerPictureStore getInstance() {
        return INSTANCE;
    }
}
