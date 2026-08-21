package me.chrr.camerapture.picture;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum PictureQuality implements StringRepresentable {
    THUMBNAIL("thumbnail"),
    FULL("full");

    public static final Codec<PictureQuality> CODEC = StringRepresentable.fromEnum(PictureQuality::values);

    private final String name;

    PictureQuality(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
