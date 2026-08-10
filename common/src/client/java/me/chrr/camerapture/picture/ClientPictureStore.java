package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.util.ImageUtil;
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

import java.util.concurrent.ConcurrentLinkedQueue;

/// The client-side picture store. This class manages picture son the client side.
/// Its cache is cleared when you leave a world. It also manages caching pictures
/// to disk when that's enabled, and converts them to NativeImages for Minecraft
/// to understand. To the outside, this class works with BufferedImages.
public class ClientPictureStore {
    private static final ClientPictureStore INSTANCE = new ClientPictureStore();

    /// Roughly how much VRAM we're willing to hold in picture textures. Bounded by bytes rather than by
    /// picture count because sizes vary enormously — a single picture can be maxImageResolution² (1920²
    /// by default, about 15MB as RGBA), so any fixed count is either wasteful or far too tight.
    private static final long MAX_TEXTURE_BYTES = 512L * 1024L * 1024L;

    /// A picture asked for more recently than this is treated as on-screen and is never evicted. Without
    /// this, standing in an area holding more pictures than the budget would evict textures that are
    /// about to be drawn again next frame, and each eviction costs a fresh request to the server — a
    /// thrash loop far more expensive than simply going over budget. Exceeding the budget is the safer
    /// failure, so that's the one we choose.
    private static final long IN_USE_GRACE_MS = 5_000L;

    private final Queue<QueuedBytes> byteQueue = new ConcurrentLinkedQueue<>();

    /// Access-ordered, guarded by its own monitor. Always go through the helpers below so eviction stays
    /// paired with releasing the texture.
    private final LinkedHashMap<UUID, RemotePicture> pictures = new LinkedHashMap<>(256, 0.75f, true);
    private long textureBytes = 0L;

    private ClientPictureStore() {
    }

    /// Look a picture up, marking it as recently used.
    private RemotePicture getCached(UUID id) {
        synchronized (pictures) {
            RemotePicture picture = pictures.get(id);
            if (picture != null) {
                picture.touch();
            }
            return picture;
        }
    }

    /// Store a picture, then evict least-recently-used entries until we're back under budget. The
    /// eviction is completed before any texture is released, so a release can never be issued for an
    /// entry still in the map.
    private void putCached(UUID id, RemotePicture picture) {
        List<Map.Entry<UUID, RemotePicture>> evicted = null;

        synchronized (pictures) {
            RemotePicture previous = pictures.put(id, picture);
            if (previous != null) {
                textureBytes -= previous.getTextureBytes();
            }
            textureBytes += picture.getTextureBytes();

            long now = System.currentTimeMillis();
            Iterator<Map.Entry<UUID, RemotePicture>> iterator = pictures.entrySet().iterator();
            while (textureBytes > MAX_TEXTURE_BYTES && iterator.hasNext()) {
                // Access order puts the least recently used entry first, so once we reach one that's
                // still in use every remaining entry is newer still and there's nothing left to free.
                Map.Entry<UUID, RemotePicture> eldest = iterator.next();
                if (now - eldest.getValue().getLastAccess() < IN_USE_GRACE_MS) {
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
            for (Map.Entry<UUID, RemotePicture> entry : evicted) {
                releaseTexture(entry.getKey(), entry.getValue());
            }
        }
    }

    /// A picture's size isn't known until its bytes are decoded, so the budget has to be corrected once
    /// the real dimensions arrive.
    private void resized(RemotePicture picture, long previousBytes) {
        synchronized (pictures) {
            textureBytes += picture.getTextureBytes() - previousBytes;
        }
    }

    /// Free an evicted picture's texture on the render thread.
    private void releaseTexture(UUID id, RemotePicture picture) {
        if (picture.getTextureIdentifier() == null) {
            return;
        }

        Minecraft.getInstance().executeIfPossible(() -> {
            // If it was requested again between eviction and now, that fetch owns the texture identifier
            // and its own registration will replace whatever is there. Leave it alone.
            if (getCached(id) != null) {
                return;
            }

            Minecraft.getInstance()
                    .getTextureManager()
                    .release(picture.getTextureIdentifier());
        });
    }

    /// Clear all the pictures from the picture store, and destroy all textures.
    public void clear() {
        Minecraft.getInstance().executeIfPossible(() -> {
            synchronized (pictures) {
                for (RemotePicture picture : pictures.values()) {
                    if (picture.getTextureIdentifier() != null) {
                        Minecraft.getInstance()
                                .getTextureManager()
                                .release(picture.getTextureIdentifier());
                    }
                }

                pictures.clear();
                textureBytes = 0L;
            }
        });
    }

    public void processReceivedError(UUID id) {
        RemotePicture picture = getCached(id);
        if (picture == null) {
            return;
        }

        picture.setStatus(RemotePicture.Status.ERROR);
        Camerapture.LOGGER.error("remote error for image {}", id);
    }

    /// An absolute ceiling on what we'll decode, whatever the server says its limit is. The server's
    /// limit arrives over the network, so it isn't something a client should stake its process on: at
    /// WebP's 16383 maximum a single picture costs over a gigabyte of heap plus as much again natively,
    /// which is not survivable. 8192 is far past any sensible poster and stays recoverable.
    private static final int ABSOLUTE_MAX_RESOLUTION = 8192;

    /// Decode WebP bytes, refusing anything whose header declares a size we don't want to allocate for.
    ///
    /// The server rejects oversized uploads, but that only covers pictures stored after the check
    /// existed — a world carried over from before it can still hold one, and a client shouldn't take
    /// the server's word for it in any case. Reading the header is cheap; being wrong is not.
    private static BufferedImage decodeChecked(UUID id, byte[] bytes) throws IOException {
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

    /// Request a picture with an ID to be fetched from the server.
    private void fetchPicture(UUID id) {
        Camerapture.EXECUTOR.execute(() -> {
            File file = getCacheFilePath(id).toFile();
            if (file.exists()) {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    processReceivedImage(id, decodeChecked(id, bytes));
                    return;
                } catch (IOException e) {
                    // If this fails, we fall through to requesting the picture from the server.
                    Camerapture.LOGGER.error("could not read cached picture {}", id, e);
                }
            }

            Camerapture.NETWORK.sendToServer(new RequestDownloadPacket(id));
        });
    }

    /// Cache picture as bytes on the client side to be re-used later.
    public void cacheBytesToDisk(UUID id, byte[] bytes) {
        if (!shouldCacheToDisk()) {
            return;
        }

        try {
            Path path = getCacheFilePath(id);
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
            Camerapture.LOGGER.error("could not cache picture {}", id, e);
        }
    }

    /// Update the stored remote picture with the given ID to correspond to
    /// the given BufferedImage. This function will convert it to a native
    /// image, upload it as a texture and change the status of the remote picture.
    public void processReceivedImage(UUID id, BufferedImage image) {
        RemotePicture picture = ensureCached(id);

        long previousBytes = picture.getTextureBytes();
        picture.setSize(image.getWidth(), image.getHeight());
        resized(picture, previousBytes);

        @SuppressWarnings("resource") NativeImage nativeImage = ImageUtil.toNativeImage(image);

        Minecraft.getInstance().executeIfPossible(() -> {
            DynamicTexture texture = new DynamicTexture(() -> "camerapture/" + id, nativeImage);
            Minecraft.getInstance()
                    .getTextureManager()
                    .register(picture.getTextureIdentifier(), texture);
            picture.setStatus(RemotePicture.Status.SUCCESS);
        });
    }

    /// Process the bytes received from the server, and update the stored picture.
    public void processReceivedBytes(UUID id, byte[] bytes) {
        byteQueue.add(new QueuedBytes(id, bytes));
    }

    /// Get a picture by UUID, fetching it from the server if we don't have it yet.
    /// This method returns null if the input UUID is null.
    ///
    /// If the corresponding picture has an error status on the client-side, this
    /// method will force-retry fetching the picture from the server.
    public RemotePicture ensureRemotePicture(@NotNull UUID id) {
        RemotePicture picture = getCached(id);
        if (picture == null || picture.getStatus() == RemotePicture.Status.ERROR) {
            picture = new RemotePicture(id);
            putCached(id, picture);
            fetchPicture(id);
        }

        return picture;
    }

    /// Get an existing entry, or create a pending one without kicking off a fetch.
    private RemotePicture ensureCached(UUID id) {
        synchronized (pictures) {
            RemotePicture existing = pictures.get(id);
            if (existing != null) {
                return existing;
            }
        }

        RemotePicture picture = new RemotePicture(id);
        putCached(id, picture);
        return picture;
    }

    /// Get a picture by UUID, fetching it from the server if we don't have it yet.
    /// This method returns null if the input UUID is null.
    ///
    /// This is called every frame for every visible picture, which is what keeps rendered pictures at the
    /// recently-used end of the cache and therefore safe from eviction.
    public RemotePicture getServerPicture(@NotNull UUID id) {
        return Optional.ofNullable(getCached(id))
                .orElseGet(() -> ensureRemotePicture(id));
    }

    /// Processes all images from the queue.
    public void processQueue() {
        QueuedBytes item;
        while ((item = byteQueue.poll()) != null) {
            final QueuedBytes queuedItem = item;
            Camerapture.EXECUTOR.execute(() -> {
                try {
                    processReceivedImage(queuedItem.id, decodeChecked(queuedItem.id, queuedItem.bytes));
                    cacheBytesToDisk(queuedItem.id, queuedItem.bytes);
                } catch (Exception e) {
                    Camerapture.LOGGER.error("failed to decode received image bytes for image {}", queuedItem.id, e);
                    RemotePicture picture = ensureCached(queuedItem.id);
                    picture.setStatus(RemotePicture.Status.ERROR);
                }
            });
        }
    }

    private static boolean shouldCacheToDisk() {
        // We enable single-player picture caching when Replay Mod is installed.
        return CameraptureClient.replayModInstalled
                || (Camerapture.CONFIG_MANAGER.getConfig().client.cachePictures
                && !Minecraft.getInstance().hasSingleplayerServer());
    }

    private Path getCacheFilePath(UUID uuid) {
        Path cacheFolder = Camerapture.PLATFORM.getGameFolder()
                .resolve("camerapture")
                .resolve("picture-cache");

        return cacheFolder.resolve(uuid + ".webp");
    }

    public static ClientPictureStore getInstance() {
        return INSTANCE;
    }

    private record QueuedBytes(UUID id, byte[] bytes) {
    }
}