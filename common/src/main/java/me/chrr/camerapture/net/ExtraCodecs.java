package me.chrr.camerapture.net;

import com.mojang.serialization.Codec;

import java.nio.ByteBuffer;

public enum ExtraCodecs {
    ;
    public static final Codec<byte[]> BYTE_ARRAY = Codec.BYTE_BUFFER.xmap(ByteBuffer::array, ByteBuffer::wrap);
}
