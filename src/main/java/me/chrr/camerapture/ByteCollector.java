package me.chrr.camerapture;

import java.util.function.Consumer;

/**
 * A utility class to deal with segmented byte streams. It can take in segments
 * of bytes, along with the amount of bytes left, and reconstructs the final byte
 * array.
 * <p>
 * On the way, it also checks if the passed number of bytes stays correct, and if
 * it receives more or less bytes than expected, will return an error.
 */
public class ByteCollector {
    private final Consumer<byte[]> callback;

    private byte[] bytes = null;
    private int offset = 0;

    public ByteCollector(Consumer<byte[]> callback) {
        this.callback = callback;
    }

    public int getCurrentLength() {
        return offset;
    }

    public boolean push(byte[] bytes, int bytesLeft) {
        if (this.bytes == null) {
            this.bytes = new byte[bytes.length + bytesLeft];
        }

        if (offset + bytes.length + bytesLeft != this.bytes.length) {
            return false;
        }

        System.arraycopy(bytes, 0, this.bytes, offset, bytes.length);
        offset += bytes.length;

        if (bytesLeft == 0) {
            callback.accept(this.bytes);
        }

        return true;
    }

    public static void split(byte[] bytes, int sectionSize, SectionConsumer callback) {
        int bytesLeft = bytes.length;
        int offset = 0;
        while (bytesLeft > 0) {
            int size = Math.min(sectionSize, bytesLeft);
            byte[] section = new byte[size];
            System.arraycopy(bytes, offset, section, 0, size);
            callback.accept(section, bytesLeft - size);

            offset += size;
            bytesLeft -= size;
        }
    }

    public interface SectionConsumer {
        void accept(byte[] section, int bytesLeft);
    }
}
