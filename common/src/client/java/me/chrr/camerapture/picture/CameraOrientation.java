package me.chrr.camerapture.picture;

import net.minecraft.network.chat.Component;

/// Whether the captured picture should be landscape or portrait.
public enum CameraOrientation {
    LANDSCAPE,
    PORTRAIT;

    public CameraOrientation next() {
        return this == LANDSCAPE ? PORTRAIT : LANDSCAPE;
    }

    public Component getLabel() {
        return Component.translatable("text.camerapture.orientation." + this.name().toLowerCase());
    }
}
