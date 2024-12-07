package me.chrr.camerapture.net;

import com.mojang.serialization.Codec;
import net.minecraft.util.Identifier;

public record NetCodec<T>(Identifier id, Codec<T> codec) {
}
