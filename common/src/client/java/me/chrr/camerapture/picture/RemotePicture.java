package me.chrr.camerapture.picture;

import me.chrr.camerapture.Camerapture;
import net.minecraft.util.Identifier;

import java.util.UUID;

/// A picture that we assume exists on the server. A remote picture
/// can either be still fetching, successfully fetched or failed to
/// fetch. If a picture is fetched, we also know the texture identifier.
public class RemotePicture {
    private Status status = Status.FETCHING;

    private final Identifier textureIdentifier;

    private int width = 0;
    private int height = 0;

    public RemotePicture(UUID id) {
        this.textureIdentifier = Camerapture.id("pictures/" + id.toString());
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