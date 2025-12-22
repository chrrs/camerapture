package me.chrr.camerapture.net;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public enum ExtraCodecs {
    ;
    public static final Codec<byte[]> BYTE_ARRAY = Codec.BYTE_BUFFER.xmap(ByteBuffer::array, ByteBuffer::wrap);

    public static <T> Codec<T> unit(Supplier<T> supplier) {
        return Codec.of(
                Encoder.empty(),
                Decoder.unit(supplier.get())
        ).codec();
    }
}
