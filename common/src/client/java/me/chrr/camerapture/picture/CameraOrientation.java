package me.chrr.camerapture.picture;

/// Whether the captured picture should be landscape or portrait.
public enum CameraOrientation {
    LANDSCAPE,
    PORTRAIT;

    public CameraOrientation next() {
        return this == LANDSCAPE ? PORTRAIT : LANDSCAPE;
    }

    public String translationKey() {
        return "text.camerapture.orientation." + this.name().toLowerCase();
    }
}
