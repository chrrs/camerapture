package me.chrr.camerapture;

import me.chrr.camerapture.net.*;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.DisplayRenderer;
import me.chrr.camerapture.render.PictureEntityRenderer;
import me.chrr.camerapture.screen.DisplayScreen;
import me.chrr.camerapture.screen.PictureScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CameraptureClient implements ClientModInitializer {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitializeClient() {
        BlockEntityRendererFactories.register(Camerapture.DISPLAY_BLOCK_ENTITY, DisplayRenderer::new);
        HandledScreens.register(Camerapture.DISPLAY_SCREEN_HANDLER, DisplayScreen::new);

        EntityRendererRegistry.register(Camerapture.PICTURE_ENTITY, PictureEntityRenderer::new);

        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (Camerapture.isCameraActive(player)) {
                ClientPlayNetworking.send(new TakePicturePacket());
                return true;
            } else {
                return false;
            }
        });

        // Server requests to show picture on screen, most likely by right-clicking a picture item
        ClientPlayNetworking.registerGlobalReceiver(ShowPicturePacket.TYPE, (packet, player, sender) ->
                MinecraftClient.getInstance().setScreen(new PictureScreen(packet.uuid())));

        // Server requests to take a picture, most likely by using a camera
        ClientPlayNetworking.registerGlobalReceiver(RequestPicturePacket.TYPE, (packet, player, sender) ->
                PictureTaker.getInstance().takePicture(packet.uuid()));

        // Server sends back a picture following a picture request by UUID
        Map<UUID, ByteCollector> collectors = new HashMap<>();
        ClientPlayNetworking.registerGlobalReceiver(PartialPicturePacket.TYPE, (packet, player, sender) -> {
            ByteCollector collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                collectors.remove(uuid);
                ThreadPooler.run(() -> ClientPictureStore.getInstance().processReceivedBytes(uuid, bytes));
            }));

            if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                LOGGER.error("received malformed byte section from server");
                ClientPictureStore.getInstance().processReceivedError(packet.uuid());
            }
        });

        // Server sends back an error following a picture request by UUID
        ClientPlayNetworking.registerGlobalReceiver(PictureErrorPacket.TYPE, (packet, player, sender) -> {
            ClientPictureStore.getInstance().processReceivedError(packet.uuid());
            collectors.remove(packet.uuid());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                ClientPictureStore.getInstance().clearCache());
    }
}
