package me.chrr.camerapture;

import java.util.function.Consumer;

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

    public static void split(byte[] bytes, int size, SectionCallback callback) {
        int bytesLeft = bytes.length;
        int offset = 0;
        while (bytesLeft > 0) {
            int sectionSize = Math.min(size, bytesLeft);
            byte[] section = new byte[sectionSize];
            System.arraycopy(bytes, offset, section, 0, sectionSize);
            callback.accept(section, bytesLeft - sectionSize);

            offset += sectionSize;
            bytesLeft -= sectionSize;
        }
    }

    public interface SectionCallback {
        void accept(byte[] section, int bytesLeft);
    }
}
