package me.chrr.camerapture.compat;

import dev.tr7zw.firstperson.api.ActivationHandler;
import dev.tr7zw.firstperson.api.FirstPersonAPI;
import me.chrr.camerapture.Camerapture;
import net.minecraft.client.MinecraftClient;

public class FirstPersonModelCompat implements ActivationHandler {
    private FirstPersonModelCompat() {
    }

    @Override
    public boolean preventFirstperson() {
        // Prevent the first person model from showing when a camera is active.
        return Camerapture.hasActiveCamera(MinecraftClient.getInstance().player);
    }

    public static void register() {
        FirstPersonAPI.registerPlayerHandler(new FirstPersonModelCompat());
    }
}
