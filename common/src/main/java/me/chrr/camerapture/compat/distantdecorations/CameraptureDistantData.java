package me.chrr.camerapture.compat.distantdecorations;

import net.minecraft.core.Direction;

import java.util.UUID;

public record CameraptureDistantData(
    UUID pictureId,
    Direction facing,
    int width,
    int height,
    int rotation,
    boolean isGlowing
) {}
