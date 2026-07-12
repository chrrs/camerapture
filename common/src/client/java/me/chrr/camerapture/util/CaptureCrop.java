package me.chrr.camerapture.util;

/// Shared math for cropping the framebuffer / viewfinder to a target aspect ratio.
public final class CaptureCrop {
    private CaptureCrop() {
    }

    public record Rect(int x, int y, int width, int height) {
    }

    /// Center-crop a rectangle of {@code sourceWidth}×{@code sourceHeight} to {@code widthOverHeight}.
    public static Rect centerCrop(int sourceWidth, int sourceHeight, double widthOverHeight) {
        double sourceRatio = (double) sourceWidth / (double) sourceHeight;

        int cropWidth;
        int cropHeight;
        if (sourceRatio > widthOverHeight) {
            cropHeight = sourceHeight;
            cropWidth = Math.max(1, (int) Math.round(sourceHeight * widthOverHeight));
        } else {
            cropWidth = sourceWidth;
            cropHeight = Math.max(1, (int) Math.round(sourceWidth / widthOverHeight));
        }

        int x = (sourceWidth - cropWidth) / 2;
        int y = (sourceHeight - cropHeight) / 2;
        return new Rect(x, y, cropWidth, cropHeight);
    }
}
