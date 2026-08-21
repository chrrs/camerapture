package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.render.CameraptureDebugStats;
import me.chrr.camerapture.util.ImageUtil;
import me.chrr.camerapture.util.NativeImageUtil;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/// The client-side picture store. It manages picture resources on the client side,
/// separating Full and Thumbnail texture caches with independent VRAM budgets, grace periods,
/// and LRU tracking.
public class ClientPictureStore {
    private static final ClientPictureStore INSTANCE = new ClientPictureStore();

    /// An absolute ceiling on what we'll decode.
    private static final int ABSOLUTE_MAX_RESOLUTION = 8192;
    private static final int ABSOLUTE_MAX_THUMBNAIL_RESOLUTION = 512;

    private final Queue<QueuedBytes> byteQueue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, RemotePicture> pictures = new ConcurrentHashMap<>();
    private final Set<PictureKey> inFlightFetches = ConcurrentHashMap.newKeySet();

    private final TextureCache fullCache = new TextureCache(PictureQuality.FULL);
    private final TextureCache thumbnailCache = new TextureCache(PictureQuality.THUMBNAIL);

    private ClientPictureStore() {
    }

    private TextureCache getCache(PictureQuality quality) {
        return (quality == PictureQuality.THUMBNAIL) ? thumbnailCache : fullCache;
    }

    /// Retrieve or create a RemotePicture entry and request the specified quality if not yet loaded.
    public RemotePicture getPicture(@NotNull UUID id, @NotNull PictureQuality quality) {
        RemotePicture picture = pictures.computeIfAbsent(id, RemotePicture::new);
        PictureTexture texture = picture.getTexture(quality);

        if (texture.getStatus() == PictureTexture.Status.SUCCESS) {
            getCache(quality).touch(id, texture);
        } else if (texture.getStatus() == PictureTexture.Status.NOT_LOADED) {
            texture.setStatus(PictureTexture.Status.FETCHING);
            fetchPicture(id, quality);
        }

        return picture;
    }

    /// Legacy compatibility helper: requests full quality picture.
    public RemotePicture getServerPicture(@NotNull UUID id) {
        return getPicture(id, PictureQuality.FULL);
    }

    /// Legacy compatibility helper: ensures full quality picture is requested.
    public RemotePicture ensureRemotePicture(@NotNull UUID id) {
        return getPicture(id, PictureQuality.FULL);
    }

    /// Request a picture with a specific quality from disk or the server.
    private void fetchPicture(UUID id, PictureQuality quality) {
        PictureKey key = new PictureKey(id, quality);
        if (!inFlightFetches.add(key)) {
            return;
        }

        Camerapture.EXECUTOR.execute(() -> {
            try {
                Path diskPath = getCacheFilePath(id, quality);
                File file = diskPath.toFile();

                // Also check legacy flat cache directory for FULL quality
                if (!file.exists() && quality == PictureQuality.FULL) {
                    File legacyFile = getLegacyCacheFilePath(id).toFile();
                    if (legacyFile.exists()) {
                        file = legacyFile;
                    }
                }

                if (file.exists()) {
                    try {
                        byte[] bytes = Files.readAllBytes(file.toPath());
                        BufferedImage image = (quality == PictureQuality.FULL)
                                ? decodeFullChecked(id, bytes)
                                : decodeThumbnailChecked(id, bytes);
                        processReceivedImage(id, quality, image);
                        return;
                    } catch (Exception e) {
                        Camerapture.LOGGER.error("could not read cached picture {} ({})", id, quality, e);
                    }
                }

                CameraptureDebugStats.recordRequest(quality);
                Camerapture.NETWORK.sendToServer(new RequestDownloadPacket(id, quality));
            } finally {
                inFlightFetches.remove(key);
            }
        });
    }

    /// Update the stored texture with the given BufferedImage and upload to GPU.
    /// Byte accounting is handled strictly by TextureCache#put when the texture is uploaded.
    public void processReceivedImage(UUID id, PictureQuality quality, BufferedImage image) {
        RemotePicture picture = pictures.computeIfAbsent(id, RemotePicture::new);
        PictureTexture texture = picture.getTexture(quality);

        texture.setSize(image.getWidth(), image.getHeight());

        @SuppressWarnings("resource") NativeImage nativeImage = NativeImageUtil.toNativeImage(image);

        Minecraft.getInstance().executeIfPossible(() -> {
            DynamicTexture dynamicTexture = new DynamicTexture(
                    () -> "camerapture/" + quality.getSerializedName() + "/" + id,
                    nativeImage
            );
            Minecraft.getInstance()
                    .getTextureManager()
                    .register(texture.getTextureIdentifier(), dynamicTexture);

            texture.setStatus(PictureTexture.Status.SUCCESS);
            getCache(quality).put(id, texture);
            CameraptureDebugStats.textureUploads.incrementAndGet();
        });
    }

    /// Process bytes received from the server by adding them to the queue.
    public void processReceivedBytes(UUID id, PictureQuality quality, byte[] bytes) {
        byteQueue.add(new QueuedBytes(id, quality, bytes));
    }

    /// Processes all images from the queue.
    public void processQueue() {
        QueuedBytes item;
        while ((item = byteQueue.poll()) != null) {
            final QueuedBytes queuedItem = item;
            Camerapture.EXECUTOR.execute(() -> {
                try {
                    BufferedImage image = (queuedItem.quality == PictureQuality.FULL)
                            ? decodeFullChecked(queuedItem.id, queuedItem.bytes)
                            : decodeThumbnailChecked(queuedItem.id, queuedItem.bytes);
                    processReceivedImage(queuedItem.id, queuedItem.quality, image);
                    cacheBytesToDisk(queuedItem.id, queuedItem.quality, queuedItem.bytes);
                } catch (Exception e) {
                    Camerapture.LOGGER.error("failed to decode received image bytes for image {} ({})", queuedItem.id, queuedItem.quality, e);
                    processReceivedError(queuedItem.id, queuedItem.quality);
                }
            });
        }
    }

    public void processReceivedError(UUID id, PictureQuality quality) {
        RemotePicture picture = pictures.get(id);
        if (picture != null) {
            picture.getTexture(quality).setStatus(PictureTexture.Status.ERROR);
        }
        CameraptureDebugStats.missingPictures.incrementAndGet();
        Camerapture.LOGGER.error("remote error for image {} ({})", id, quality);
    }

    /// Cache picture bytes to disk.
    public void cacheBytesToDisk(UUID id, PictureQuality quality, byte[] bytes) {
        if (!shouldCacheToDisk()) {
            return;
        }

        try {
            Path path = getCacheFilePath(id, quality);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            Camerapture.LOGGER.error("could not cache picture {} ({})", id, quality, e);
        }
    }

    /// Decode full WebP bytes with header safety checks.
    private static BufferedImage decodeFullChecked(UUID id, byte[] bytes) throws IOException {
        WebPHeader.Size size = WebPHeader.read(bytes);
        if (size == null) {
            throw new IOException("picture " + id + " is not a readable WebP");
        }

        int limit = Math.min(CameraptureClient.syncedConfig.maxImageResolution(), ABSOLUTE_MAX_RESOLUTION);
        if (size.width() > limit || size.height() > limit) {
            throw new IOException("refusing to decode picture " + id + " at "
                    + size.width() + "x" + size.height() + ", over the " + limit + " limit");
        }

        return ImageUtil.decodeImageFromWebP(bytes);
    }

    /// Decode thumbnail WebP bytes with defensive bounds checks.
    private static BufferedImage decodeThumbnailChecked(UUID id, byte[] bytes) throws IOException {
        WebPHeader.Size size = WebPHeader.read(bytes);
        if (size == null) {
            throw new IOException("thumbnail " + id + " is not a readable WebP");
        }

        int configuredMax = Camerapture.CONFIG_MANAGER.getConfig().client.thumbnailResolution * 2;
        int limit = Math.min(Math.max(256, configuredMax), ABSOLUTE_MAX_THUMBNAIL_RESOLUTION);
        if (size.width() > limit || size.height() > limit) {
            throw new IOException("refusing to decode thumbnail " + id + " at "
                    + size.width() + "x" + size.height() + ", over the " + limit + " limit");
        }

        return ImageUtil.decodeImageFromWebP(bytes);
    }

    /// Clear all pictures from the store and destroy all textures.
    public void clear() {
        Minecraft.getInstance().executeIfPossible(() -> {
            fullCache.clear();
            thumbnailCache.clear();
            pictures.clear();
            inFlightFetches.clear();
        });
    }

    public long getFullTextureBytes() {
        return fullCache.getTextureBytes();
    }

    public long getThumbnailTextureBytes() {
        return thumbnailCache.getTextureBytes();
    }

    private static boolean shouldCacheToDisk() {
        return CameraptureClient.replayModInstalled
                || (Camerapture.CONFIG_MANAGER.getConfig().client.cachePictures
                && !Minecraft.getInstance().hasSingleplayerServer());
    }

    private Path getCacheFilePath(UUID uuid, PictureQuality quality) {
        String subfolder = (quality == PictureQuality.THUMBNAIL) ? "thumbnails" : "full";
        return Camerapture.PLATFORM.getGameFolder()
                .resolve("camerapture")
                .resolve("picture-cache")
                .resolve(subfolder)
                .resolve(uuid + ".webp");
    }

    private Path getLegacyCacheFilePath(UUID uuid) {
        return Camerapture.PLATFORM.getGameFolder()
                .resolve("camerapture")
                .resolve("picture-cache")
                .resolve(uuid + ".webp");
    }

    public static ClientPictureStore getInstance() {
        return INSTANCE;
    }

    private record QueuedBytes(UUID id, PictureQuality quality, byte[] bytes) {
    }

    /// An LRU cache managing GPU texture memory for a specific quality level.
    private static class TextureCache {
        private final PictureQuality quality;
        private final LinkedHashMap<UUID, PictureTexture> entries = new LinkedHashMap<>(256, 0.75f, true);
        private long textureBytes = 0L;

        private TextureCache(PictureQuality quality) {
            this.quality = quality;
        }

        private long getMaxBytes() {
            long budgetMiB = (quality == PictureQuality.THUMBNAIL)
                    ? Camerapture.CONFIG_MANAGER.getConfig().client.thumbnailTextureBudgetMiB
                    : Camerapture.CONFIG_MANAGER.getConfig().client.fullTextureBudgetMiB;
            return Math.max(8L, budgetMiB) * 1024L * 1024L;
        }

        private long getInUseGraceMs() {
            // Thumbnails are small and useful across distant scenes, so they have a longer grace period.
            return (quality == PictureQuality.THUMBNAIL) ? 30_000L : 5_000L;
        }

        public synchronized long getTextureBytes() {
            return textureBytes;
        }

        public synchronized void touch(UUID id, PictureTexture texture) {
            texture.touch();
            entries.get(id); // Access in LinkedHashMap moves to MRU end
        }

        public void put(UUID id, PictureTexture texture) {
            List<Map.Entry<UUID, PictureTexture>> evicted = null;

            synchronized (this) {
                PictureTexture previous = entries.put(id, texture);
                if (previous != null) {
                    textureBytes -= previous.getTextureBytes();
                }
                textureBytes += texture.getTextureBytes();
                texture.touch();

                long now = System.currentTimeMillis();
                long maxBytes = getMaxBytes();
                long graceMs = getInUseGraceMs();

                Iterator<Map.Entry<UUID, PictureTexture>> iterator = entries.entrySet().iterator();
                while (textureBytes > maxBytes && iterator.hasNext()) {
                    Map.Entry<UUID, PictureTexture> eldest = iterator.next();
                    if (now - eldest.getValue().getLastAccess() < graceMs) {
                        break;
                    }

                    iterator.remove();
                    textureBytes -= eldest.getValue().getTextureBytes();

                    if (evicted == null) {
                        evicted = new ArrayList<>();
                    }
                    evicted.add(Map.entry(eldest.getKey(), eldest.getValue()));
                }
            }

            if (evicted != null) {
                for (Map.Entry<UUID, PictureTexture> entry : evicted) {
                    releaseTexture(entry.getKey(), entry.getValue());
                }
            }
        }

        private void releaseTexture(UUID id, PictureTexture texture) {
            CameraptureDebugStats.textureEvictions.incrementAndGet();
            texture.setStatus(PictureTexture.Status.NOT_LOADED);

            Minecraft.getInstance().executeIfPossible(() -> {
                synchronized (this) {
                    if (entries.containsKey(id)) {
                        return;
                    }
                }
                Minecraft.getInstance().getTextureManager().release(texture.getTextureIdentifier());
            });
        }

        public synchronized void clear() {
            for (PictureTexture texture : entries.values()) {
                texture.setStatus(PictureTexture.Status.NOT_LOADED);
                Minecraft.getInstance().getTextureManager().release(texture.getTextureIdentifier());
            }
            entries.clear();
            textureBytes = 0L;
        }
    }
}