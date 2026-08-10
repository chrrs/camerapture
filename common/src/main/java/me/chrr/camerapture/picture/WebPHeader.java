package me.chrr.camerapture.picture;

import org.jetbrains.annotations.Nullable;

/// Reads the pixel dimensions out of a WebP header without decoding the image.
///
/// The server stores picture bytes verbatim and never decodes them, so on its own it has no idea how
/// large a picture claims to be — only how many bytes it takes up compressed. Those are very different
/// numbers: a flat-colour image at WebP's maximum 16383x16383 compresses to a few kilobytes but costs
/// over a gigabyte of heap to decode, and the server hands that to every client that looks at it.
/// Decoding server-side to find out would mean allocating the very buffer we're trying to refuse.
///
/// All three WebP variants carry the dimensions within the first 30 bytes, so a header read is enough.
/// See RFC 9649 (WebP container) and the VP8/VP8L bitstream formats.
public final class WebPHeader {
    private WebPHeader() {
    }

    /// Enough to identify the container and its variant. Each variant then checks its own length, since
    /// they don't all need the same: rejecting a small but perfectly valid picture would be its own bug.
    private static final int RIFF_HEADER_LENGTH = 16;

    /// WebP caps each dimension at 16383.
    public static final int MAX_DIMENSION = 16383;

    public record Size(int width, int height) {
    }

    /// Returns the image's dimensions, or null if this isn't a WebP we can read. A null result means the
    /// data should be rejected rather than trusted — we can't vouch for something we can't parse.
    @Nullable
    public static Size read(byte @Nullable [] data) {
        if (data == null || data.length < RIFF_HEADER_LENGTH) {
            return null;
        }

        if (!matches(data, 0, "RIFF") || !matches(data, 8, "WEBP")) {
            return null;
        }

        if (matches(data, 12, "VP8 ")) {
            // Simple lossy: a 3-byte frame tag, the 0x9D 0x01 0x2A sync code, then two 14-bit dimensions.
            if (data.length < 30) {
                return null;
            }

            if (u8(data, 23) != 0x9D || u8(data, 24) != 0x01 || u8(data, 25) != 0x2A) {
                return null;
            }

            return new Size(u16(data, 26) & 0x3FFF, u16(data, 28) & 0x3FFF);
        }

        if (matches(data, 12, "VP8L")) {
            // Simple lossless: a signature byte, then width-1 and height-1 as 14 bits each.
            if (data.length < 25 || u8(data, 20) != 0x2F) {
                return null;
            }

            int bits = u32(data, 21);
            return new Size((bits & 0x3FFF) + 1, ((bits >>> 14) & 0x3FFF) + 1);
        }

        if (matches(data, 12, "VP8X")) {
            // Extended: a flags byte, 3 reserved bytes, then canvas width-1 and height-1 as 24 bits each.
            if (data.length < 30) {
                return null;
            }

            return new Size(u24(data, 24) + 1, u24(data, 27) + 1);
        }

        return null;
    }

    private static boolean matches(byte[] data, int offset, String fourCC) {
        for (int i = 0; i < 4; i++) {
            if (u8(data, offset + i) != fourCC.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    private static int u8(byte[] data, int offset) {
        return data[offset] & 0xFF;
    }

    private static int u16(byte[] data, int offset) {
        return u8(data, offset) | (u8(data, offset + 1) << 8);
    }

    private static int u24(byte[] data, int offset) {
        return u16(data, offset) | (u8(data, offset + 2) << 16);
    }

    private static int u32(byte[] data, int offset) {
        return u24(data, offset) | (u8(data, offset + 3) << 24);
    }
}
