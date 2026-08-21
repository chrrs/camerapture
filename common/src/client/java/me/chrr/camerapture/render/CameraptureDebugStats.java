package me.chrr.camerapture.render;

import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureQuality;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class CameraptureDebugStats {
    private CameraptureDebugStats() {
    }

    // Frame-level counters (reset or sampled per frame/tick)
    public static final AtomicInteger extractedFrames = new AtomicInteger();
    public static final AtomicInteger submittedFrames = new AtomicInteger();
    public static final AtomicInteger frustumRejected = new AtomicInteger();
    public static final AtomicInteger subpixelRejected = new AtomicInteger();
    public static final AtomicInteger thumbnailLodFrames = new AtomicInteger();
    public static final AtomicInteger fullLodFrames = new AtomicInteger();

    // Actual GPU texture render counts (distinguishes thumbnail fallback from full textures)
    public static final AtomicInteger thumbnailTextureRenders = new AtomicInteger();
    public static final AtomicInteger fullTextureRenders = new AtomicInteger();
    public static final AtomicInteger placeholderRenders = new AtomicInteger();
    public static final AtomicInteger missingPictures = new AtomicInteger();

    // Cumulative / persistent counters
    public static final AtomicInteger textureUploads = new AtomicInteger();
    public static final AtomicInteger textureEvictions = new AtomicInteger();
    public static final AtomicInteger thumbnailRequests = new AtomicInteger();
    public static final AtomicInteger fullRequests = new AtomicInteger();
    public static final AtomicLong networkBytes = new AtomicLong();

    public static void recordRequest(PictureQuality quality) {
        if (quality == PictureQuality.THUMBNAIL) {
            thumbnailRequests.incrementAndGet();
        } else {
            fullRequests.incrementAndGet();
        }
    }

    public static void recordBytes(long bytes) {
        networkBytes.addAndGet(bytes);
    }

    public static void resetFrameCounters() {
        extractedFrames.set(0);
        submittedFrames.set(0);
        frustumRejected.set(0);
        subpixelRejected.set(0);
        thumbnailLodFrames.set(0);
        fullLodFrames.set(0);
        thumbnailTextureRenders.set(0);
        fullTextureRenders.set(0);
        placeholderRenders.set(0);
        missingPictures.set(0);
    }

    public static String getSummary() {
        long fullVramMiB = ClientPictureStore.getInstance().getFullTextureBytes() / (1024L * 1024L);
        long thumbVramKiB = ClientPictureStore.getInstance().getThumbnailTextureBytes() / 1024L;
        double netMiB = networkBytes.get() / (1024.0 * 1024.0);

        return String.format(
                "Camerapture Telemetry:\n" +
                "  Frames: [Extracted: %d, Frustum Culled: %d, Subpixel Culled: %d, Thumb LOD: %d, Full LOD: %d]\n" +
                "  Renders: [Thumb Textures: %d, Full Textures: %d, Placeholders: %d]\n" +
                "  VRAM: [Full: %d MiB, Thumb: %d KiB] | Cache: [Uploads: %d, Evictions: %d]\n" +
                "  Network: [Thumb Req: %d, Full Req: %d, Transferred: %.2f MiB]",
                extractedFrames.get(), frustumRejected.get(), subpixelRejected.get(),
                thumbnailLodFrames.get(), fullLodFrames.get(),
                thumbnailTextureRenders.get(), fullTextureRenders.get(), placeholderRenders.get(),
                fullVramMiB, thumbVramKiB, textureUploads.get(), textureEvictions.get(),
                thumbnailRequests.get(), fullRequests.get(), netMiB
        );
    }
}
