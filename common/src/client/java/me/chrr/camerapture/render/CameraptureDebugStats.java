package me.chrr.camerapture.render;

import me.chrr.camerapture.picture.PictureQuality;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class CameraptureDebugStats {
    private CameraptureDebugStats() {
    }

    // Frame-level counters (reset or sampled per frame)
    public static final AtomicInteger extractedFrames = new AtomicInteger();
    public static final AtomicInteger submittedFrames = new AtomicInteger();
    public static final AtomicInteger frustumRejected = new AtomicInteger();
    public static final AtomicInteger subpixelRejected = new AtomicInteger();
    public static final AtomicInteger thumbnailRenders = new AtomicInteger();
    public static final AtomicInteger fullRenders = new AtomicInteger();
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
        thumbnailRenders.set(0);
        fullRenders.set(0);
        missingPictures.set(0);
    }

    public static String getSummary() {
        return String.format(
                "Camerapture Stats: [Extracted: %d, Submitted: %d, Frustum Culled: %d, Subpixel Culled: %d, Thumbs Rendered: %d, Full Rendered: %d] | [Uploads: %d, Evictions: %d, Thumb Req: %d, Full Req: %d, Net Bytes: %d]",
                extractedFrames.get(), submittedFrames.get(), frustumRejected.get(), subpixelRejected.get(),
                thumbnailRenders.get(), fullRenders.get(), textureUploads.get(), textureEvictions.get(),
                thumbnailRequests.get(), fullRequests.get(), networkBytes.get()
        );
    }
}
