package me.chrr.camerapture.net;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;

public record NetCodec<T>(Identifier id, Codec<T> codec) {
}
