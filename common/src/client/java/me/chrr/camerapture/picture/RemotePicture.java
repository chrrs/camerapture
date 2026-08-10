package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/// A picture that we assume exists on the server. A remote picture
/// can either be still fetching, successfully fetched or failed to
/// fetch. If a picture is fetched, we also know the texture identifier.
public class RemotePicture {
    private Status status = Status.FETCHING;

    private final Identifier textureIdentifier;

    private int width = 0;
    private int height = 0;

    /// When this picture was last asked for. The store uses it to tell pictures that are currently on
    /// screen apart from ones merely still cached, so it never evicts a texture that's in use.
    private volatile long lastAccess = System.currentTimeMillis();

    public RemotePicture(UUID id) {
        this.textureIdentifier = Camerapture.id("pictures/" + id.toString());
    }

    void touch() {
        this.lastAccess = System.currentTimeMillis();
    }

    long getLastAccess() {
        return lastAccess;
    }

    /// Roughly what this picture costs on the GPU, as RGBA. Zero until the size is known.
    long getTextureBytes() {
        return (long) width * height * 4L;
    }

    public Status getStatus() {
        return status;
    }

    public Identifier getTextureIdentifier() {
        return textureIdentifier;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    void setStatus(Status status) {
        this.status = status;
    }

    void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public enum Status {
        FETCHING,
        SUCCESS,
        ERROR,
    }
}