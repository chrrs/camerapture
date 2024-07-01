package me.chrr.camerapture;

import com.luciad.imageio.webp.WebP;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.*;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.serverbound.SyncConfigPacket;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.PictureFrameEntityRenderer;
import me.chrr.camerapture.screen.AlbumScreen;
import me.chrr.camerapture.screen.PictureFrameScreen;
import me.chrr.camerapture.screen.PictureScreen;
import me.chrr.camerapture.screen.UploadScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.TypedActionResult;

import javax.imageio.ImageIO;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraptureClient implements ClientModInitializer {
    public static boolean replayModInstalled = false;

    @Override
    public void onInitializeClient() {
        // FIXME: Workaround for an Essential issue, it seems like it doesn't
        //        detect the webp-imageio library in the first pass.
        ImageIO.scanForPlugins();

        if (!WebP.loadNativeLibrary()) {
            Camerapture.LOGGER.error("failed to load ImageIO-WebP, pictures might not work!");
        }

        ClientPictureStore.getInstance().clearCache();
        PictureTaker.getInstance().resetConfig();

        HandledScreens.register(Camerapture.ALBUM_SCREEN_HANDLER, AlbumScreen::new);
        EntityRendererRegistry.register(Camerapture.PICTURE_FRAME, PictureFrameEntityRenderer::new);

        HandledScreens.register(Camerapture.PICTURE_FRAME_SCREEN_HANDLER, PictureFrameScreen::new);

        if (FabricLoader.getInstance().isModLoaded("replaymod")) {
            Camerapture.LOGGER.info("Replay Mod is detected, Camerapture will cache pictures, regardless of config");
            replayModInstalled = true;
        }

        registerPackets();
        registerEvents();
    }

    private void registerPackets() {
        ClientNetworking.init();

        // Server requests client to send over a picture, most likely from the camera
        ClientNetworking.onClientPacketReceive(RequestUploadPacket.class, (packet) ->
                ThreadPooler.run(() -> PictureTaker.getInstance().sendStoredPicture(packet.uuid())));

        // Server sends back a picture following a picture request by UUID
        Map<UUID, ByteCollector> collectors = new ConcurrentHashMap<>();
        ClientNetworking.onClientPacketReceive(DownloadPartialPicturePacket.class, (packet) -> {
            ByteCollector collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                collectors.remove(uuid);
                ThreadPooler.run(() -> ClientPictureStore.getInstance().processReceivedBytes(uuid, bytes));
            }));

            if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                Camerapture.LOGGER.error("received malformed byte section from server");
                ClientPictureStore.getInstance().processReceivedError(packet.uuid());
            }
        });

        // Server sends back an error following a picture request by UUID
        ClientNetworking.onClientPacketReceive(PictureErrorPacket.class, (packet) -> {
            ClientPictureStore.getInstance().processReceivedError(packet.uuid());
            collectors.remove(packet.uuid());
        });

        // Server sends over the server-side config
        ClientNetworking.onClientPacketReceive(SyncConfigPacket.class, (packet) ->
                PictureTaker.getInstance().setConfig(packet.maxImageBytes(), packet.maxImageResolution()));
    }

    private void registerEvents() {
        // Attacking when a camera is active should take a picture!
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (Camerapture.hasActiveCamera(player) && paperInInventory() > 0) {
                PictureTaker.getInstance().uploadScreenPicture();
                return true;
            }

            return false;
        });

        // Right-clicking on certain items should open client-side GUI's.
        // Ideally, this would be done using the methods in the Item class,
        // but we can't access client code from there.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != player) {
                return TypedActionResult.pass(stack);
            }

            if (stack.isOf(Camerapture.PICTURE)) {
                // Right-clicking a picture item should open the picture screen.
                if (PictureItem.getPictureData(stack) != null) {
                    client.submit(() -> client.setScreen(new PictureScreen(List.of(stack))));
                    return TypedActionResult.success(stack);
                }
            } else if (stack.isOf(Camerapture.ALBUM) && !player.isSneaking()) {
                // Right-clicking the album should open the gallery screen.
                List<ItemStack> pictures = AlbumItem.getPictures(stack);
                if (!pictures.isEmpty()) {
                    client.submit(() -> client.setScreen(new PictureScreen(pictures)));
                    return TypedActionResult.success(stack);
                }
            } else if (player.isSneaking()
                    && stack.isOf(Camerapture.CAMERA)
                    && !CameraItem.isActive(stack)
                    && !player.getItemCooldownManager().isCoolingDown(Camerapture.CAMERA)) {
                // Shift-right clicking the camera should open the upload screen.
                client.submit(() -> client.setScreen(new UploadScreen()));
                return TypedActionResult.success(stack);
            }

            return TypedActionResult.pass(stack);
        });

        // When disconnecting, we clear the picture cache.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPictureStore.getInstance().clearCache();
            PictureTaker.getInstance().resetConfig();
        });
    }

    /**
     * Returns the amount of paper in the player's inventory.
     */
    public static int paperInInventory() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client.player == null ? 0 : client.player.getInventory().count(Items.PAPER);
    }
}
