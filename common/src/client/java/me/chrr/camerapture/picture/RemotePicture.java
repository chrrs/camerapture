package me.chrr.camerapture.picture;

import net.minecraft.resources.Identifier;

import java.util.UUID;

/// Represents a remote picture on the client. It separates metadata from individual texture qualities
/// (THUMBNAIL vs FULL), enabling independent residency, promotion, and fallback.
public class RemotePicture {
    private final UUID id;
    private final PictureTexture thumbnail;
    private final PictureTexture full;

    public RemotePicture(UUID id) {
        this.id = id;
        this.thumbnail = new PictureTexture(id, PictureQuality.THUMBNAIL);
        this.full = new PictureTexture(id, PictureQuality.FULL);
    }

    public UUID getId() {
        return id;
    }

    public PictureTexture getThumbnail() {
        return thumbnail;
    }

    public PictureTexture getFull() {
        return full;
    }

    public PictureTexture getTexture(PictureQuality quality) {
        return (quality == PictureQuality.THUMBNAIL) ? thumbnail : full;
    }

    /// Returns the best available texture for the requested quality:
    /// - If FULL is requested: returns full if SUCCESS, else thumbnail if SUCCESS (seamless fallback while promotion is pending), else full.
    /// - If THUMBNAIL is requested: returns thumbnail if SUCCESS, else full if SUCCESS, else thumbnail.
    public PictureTexture getEffectiveTexture(PictureQuality requestedQuality) {
        if (requestedQuality == PictureQuality.FULL) {
            if (full.getStatus() == PictureTexture.Status.SUCCESS) {
                return full;
            }
            if (thumbnail.getStatus() == PictureTexture.Status.SUCCESS) {
                return thumbnail;
            }
            return full;
        } else {
            if (thumbnail.getStatus() == PictureTexture.Status.SUCCESS) {
                return thumbnail;
            }
            if (full.getStatus() == PictureTexture.Status.SUCCESS) {
                return full;
            }
            return thumbnail;
        }
    }

    public PictureTexture.Status getStatus() {
        return full.getStatus();
    }

    public int getWidth() {
        if (full.getWidth() > 0) return full.getWidth();
        return thumbnail.getWidth();
    }

    public int getHeight() {
        if (full.getHeight() > 0) return full.getHeight();
        return thumbnail.getHeight();
    }

    public Identifier getTextureIdentifier() {
        return full.getTextureIdentifier();
    }
}