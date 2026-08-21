package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.util.ImageUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/// The server-side picture store. It manages picture storage, thumbnail generation,
/// and disk caching in the world folder. Pictures are identified by UUID and PictureQuality.
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
    private final LinkedHashMap<PictureKey, StoredPicture> pictureCache = new LinkedHashMap<>(512, 0.75f, true);
    private long cacheBytes = 0L;

    /// Per-resource load locks, so concurrent misses for the same picture key collapse into one disk read.
    private final Map<PictureKey, Object> loadLocks = new ConcurrentHashMap<>();

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

        // Store original
        PictureKey fullKey = new PictureKey(id, PictureQuality.FULL);
        cache(fullKey, picture);

        Path fullPath = getFilePath(server, id, PictureQuality.FULL);
        Files.createDirectories(fullPath.getParent());
        Files.write(fullPath, picture.bytes());

        // Generate and store thumbnail
        try {
            int thumbRes = Camerapture.CONFIG_MANAGER.getConfig().server.thumbnailResolution;
            byte[] thumbBytes = ImageUtil.createThumbnail(picture.bytes(), thumbRes);
            StoredPicture thumbPicture = new StoredPicture(thumbBytes);

            PictureKey thumbKey = new PictureKey(id, PictureQuality.THUMBNAIL);
            cache(thumbKey, thumbPicture);

            Path thumbPath = getFilePath(server, id, PictureQuality.THUMBNAIL);
            Files.write(thumbPath, thumbBytes);
        } catch (Exception e) {
            Camerapture.LOGGER.error("failed to generate thumbnail for picture {}", id, e);
        }
    }

    @Nullable
    public StoredPicture get(MinecraftServer server, UUID id, PictureQuality quality) throws IOException {
        PictureKey key = new PictureKey(id, quality);
        StoredPicture cached = getCached(key);
        if (cached != null) {
            return cached;
        }

        // Collapse concurrent misses for the same picture key. When a crowd walks into the same area they
        // all ask for the same posters at once; without this, every one of those requests would do its
        // own disk read and allocate its own copy of the bytes.
        Object loadLock = loadLocks.computeIfAbsent(key, k -> new Object());
        try {
            synchronized (loadLock) {
                cached = getCached(key);
                if (cached != null) {
                    return cached;
                }

                Path path = getFilePath(server, id, quality);
                if (Files.exists(path)) {
                    StoredPicture picture = new StoredPicture(Files.readAllBytes(path));
                    cache(key, picture);
                    return picture;
                }

                // If thumbnail requested but missing, check if original exists and generate lazily (migration)
                if (quality == PictureQuality.THUMBNAIL) {
                    Path fullPath = getFilePath(server, id, PictureQuality.FULL);
                    if (Files.exists(fullPath)) {
                        try {
                            byte[] originalBytes = Files.readAllBytes(fullPath);
                            int thumbRes = Camerapture.CONFIG_MANAGER.getConfig().server.thumbnailResolution;
                            byte[] thumbBytes = ImageUtil.createThumbnail(originalBytes, thumbRes);
                            StoredPicture thumbPicture = new StoredPicture(thumbBytes);

                            Files.write(path, thumbBytes);
                            cache(key, thumbPicture);
                            return thumbPicture;
                        } catch (Exception e) {
                            Camerapture.LOGGER.error("failed to generate lazy thumbnail for picture {}", id, e);
                        }
                    }
                }

                return null;
            }
        } finally {
            loadLocks.remove(key, loadLock);
        }
    }

    @Nullable
    private StoredPicture getCached(PictureKey key) {
        synchronized (pictureCache) {
            // Has to be a single lookup: containsKey/get is not atomic, so a concurrent eviction between
            // the two would report the picture as missing. It also keeps the LRU honest — the cache is
            // access-ordered, and containsKey does not count as an access.
            return pictureCache.get(key);
        }
    }

    /// Add a picture to the cache, evicting least-recently-used entries until it fits.
    private void cache(PictureKey key, StoredPicture picture) {
        synchronized (pictureCache) {
            StoredPicture previous = pictureCache.put(key, picture);
            if (previous != null) {
                cacheBytes -= previous.bytes().length;
            }
            cacheBytes += picture.bytes().length;

            // Access order puts the least recently used entry first. The entry we just inserted is the
            // most recent, so it's last and only evicted if it alone is over the cap.
            Iterator<Map.Entry<PictureKey, StoredPicture>> iterator = pictureCache.entrySet().iterator();
            while (cacheBytes > MAX_CACHE_BYTES && pictureCache.size() > 1 && iterator.hasNext()) {
                Map.Entry<PictureKey, StoredPicture> eldest = iterator.next();
                cacheBytes -= eldest.getValue().bytes().length;
                iterator.remove();
            }
        }
    }

    private Path getFilePath(MinecraftServer server, UUID uuid, PictureQuality quality) {
        Path dataFolder = server.getWorldPath(LevelResource.ROOT).resolve("camerapture");
        if (quality == PictureQuality.THUMBNAIL) {
            return dataFolder.resolve(uuid + ".thumb.webp");
        } else {
            return dataFolder.resolve(uuid + ".webp");
        }
    }

    public static ServerPictureStore getInstance() {
        return INSTANCE;
    }
}
