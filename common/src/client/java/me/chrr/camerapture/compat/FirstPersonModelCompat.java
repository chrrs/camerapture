package me.chrr.camerapture.compat;

import dev.tr7zw.firstperson.api.ActivationHandler;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import me.chrr.camerapture.item.CameraItem;
import net.minecraft.client.MinecraftClient;

public enum FirstPersonModelCompat {
    ;

    public static void register() {
        FirstPersonAPI.registerPlayerHandler((ActivationHandler) () -> {
            // Prevent the first person model from showing when a camera is active.
            return CameraItem.find(MinecraftClient.getInstance().player, true) != null;
        });
    }
}