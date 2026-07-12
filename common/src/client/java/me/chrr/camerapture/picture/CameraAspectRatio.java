package me.chrr.camerapture.picture;

/// Aspect ratio family used when capturing a picture.
/// Combined with {@link CameraOrientation} this covers screen, screen inverse,
/// 16:9, 9:16, 4:3, and 3:4.
public enum CameraAspectRatio {
    SCREEN,
    RATIO_16_9,
    RATIO_4_3;

    public CameraAspectRatio next() {
        CameraAspectRatio[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    /// Width divided by height for the chosen ratio and orientation.
    public double widthOverHeight(CameraOrientation orientation, int screenWidth, int screenHeight) {
        double ratio = switch (this) {
            case SCREEN -> (double) screenWidth / (double) screenHeight;
            case RATIO_16_9 -> 16.0 / 9.0;
            case RATIO_4_3 -> 4.0 / 3.0;
        };

        if (orientation == CameraOrientation.PORTRAIT) {
            ratio = 1.0 / ratio;
        }

        return ratio;
    }

    public String translationKey(CameraOrientation orientation) {
        return switch (this) {
            case SCREEN -> orientation == CameraOrientation.PORTRAIT
                    ? "text.camerapture.aspect.screen_inverse"
                    : "text.camerapture.aspect.screen";
            case RATIO_16_9 -> orientation == CameraOrientation.PORTRAIT
                    ? "text.camerapture.aspect.9_16"
                    : "text.camerapture.aspect.16_9";
            case RATIO_4_3 -> orientation == CameraOrientation.PORTRAIT
                    ? "text.camerapture.aspect.3_4"
                    : "text.camerapture.aspect.4_3";
        };
    }
}
