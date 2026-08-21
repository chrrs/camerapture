package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/// Represents a single texture representation (THUMBNAIL or FULL) of a picture on the client.
public class PictureTexture {
    private final UUID id;
    private final PictureQuality quality;
    private final Identifier textureIdentifier;

    private Status status = Status.NOT_LOADED;
    private int width = 0;
    private int height = 0;
    private volatile long lastAccess = System.currentTimeMillis();

    public PictureTexture(UUID id, PictureQuality quality) {
        this.id = id;
        this.quality = quality;
        String prefix = (quality == PictureQuality.THUMBNAIL) ? "pictures/thumb/" : "pictures/";
        this.textureIdentifier = Camerapture.id(prefix + id.toString());
    }

    public void touch() {
        this.lastAccess = System.currentTimeMillis();
    }

    public long getLastAccess() {
        return lastAccess;
    }

    /// Roughly what this picture costs on the GPU as RGBA. Zero until the size is known.
    public long getTextureBytes() {
        return (long) width * height * 4L;
    }

    public UUID getId() {
        return id;
    }

    public PictureQuality getQuality() {
        return quality;
    }

    public Identifier getTextureIdentifier() {
        return textureIdentifier;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public enum Status {
        NOT_LOADED,
        FETCHING,
        SUCCESS,
        ERROR,
    }
}
