package me.chrr.camerapture.picture;

/// Aspect ratio family used when capturing a picture.
/// Combined with {@link CameraOrientation} this covers screen, screen inverse,
/// 16:9, 9:16, 4:3, and 3:4.
public enum CameraAspectRatio {
    SCREEN,
    RATIO_16_9,
    RATIO_4_3;

    private static final double RATIO_MATCH_EPSILON = 0.02;

    public CameraAspectRatio next(int screenWidth, int screenHeight) {
        CameraAspectRatio next = this;
        do {
            CameraAspectRatio[] values = values();
            next = values[(next.ordinal() + 1) % values.length];
        } while (!next.isAvailable(screenWidth, screenHeight) && next != this);
        return next;
    }

    /// {@code SCREEN} is hidden when the window is already 16:9 or 4:3 (or the inverse).
    public boolean isAvailable(int screenWidth, int screenHeight) {
        if (this != SCREEN) {
            return true;
        }

        double screenRatio = (double) screenWidth / (double) screenHeight;
        return !matchesKnownRatio(screenRatio, 16.0 / 9.0)
                && !matchesKnownRatio(screenRatio, 4.0 / 3.0);
    }

    /// If {@code SCREEN} is redundant for this window, map it to the matching fixed ratio.
    public CameraAspectRatio resolve(int screenWidth, int screenHeight) {
        if (this != SCREEN || isAvailable(screenWidth, screenHeight)) {
            return this;
        }

        double screenRatio = (double) screenWidth / (double) screenHeight;
        if (matchesKnownRatio(screenRatio, 16.0 / 9.0)) {
            return RATIO_16_9;
        }
        if (matchesKnownRatio(screenRatio, 4.0 / 3.0)) {
            return RATIO_4_3;
        }
        return this;
    }

    private static boolean matchesKnownRatio(double widthOverHeight, double knownRatio) {
        double landscape = widthOverHeight >= 1.0 ? widthOverHeight : 1.0 / widthOverHeight;
        double knownLandscape = knownRatio >= 1.0 ? knownRatio : 1.0 / knownRatio;
        return Math.abs(landscape - knownLandscape) <= RATIO_MATCH_EPSILON;
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
