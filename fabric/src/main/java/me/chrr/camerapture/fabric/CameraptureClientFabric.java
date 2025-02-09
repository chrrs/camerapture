package me.chrr.camerapture.fabric;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.gui.*;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.PictureFrameEntityRenderer;
import me.chrr.camerapture.render.PictureItemRenderer;
import me.chrr.camerapture.render.ShouldRenderPicture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.render.item.property.bool.BooleanProperties;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;

public class CameraptureClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        this.registerClientContent();
        this.registerClientEvents();
        CameraptureClient.registerPacketHandlers();

        CameraptureClient.init();
    }

    public void registerClientContent() {
        // Picture
        BooleanProperties.ID_MAPPER.put(Camerapture.id("should_render_picture"), ShouldRenderPicture.MAP_CODEC);
        SpecialModelTypes.ID_MAPPER.put(Camerapture.id("picture"), PictureItemRenderer.Unbaked.MAP_CODEC);

        // Picture Frame
        EntityRendererRegistry.register(Camerapture.PICTURE_FRAME, PictureFrameEntityRenderer::new);
        HandledScreens.register(Camerapture.PICTURE_FRAME_SCREEN_HANDLER, PictureFrameScreen::new);

        // Album
        HandledScreens.register(Camerapture.ALBUM_SCREEN_HANDLER, AlbumScreen::new);
        HandledScreens.register(Camerapture.ALBUM_LECTERN_SCREEN_HANDLER, AlbumLecternScreen::new);
    }

    public void registerClientEvents() {
        // When attacking with an active camera, we want to take a picture.
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            CameraItem.HeldCamera camera = CameraItem.find(player, true);
            if (camera == null) {
                return false;
            }

            if (CameraItem.canTakePicture(player)) {
                PictureTaker.getInstance().takePicture();
            }

            return true;
        });

        // Right-clicking on certain items should open client-side GUI's.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (!world.isClient()) {
                return ActionResult.PASS;
            }

            ItemStack stack = player.getStackInHand(hand);
            return CameraptureClient.onUseItem(player, stack);
        });

        // Clear cache and reset the picture taker configuration when logging out of a world.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientPictureStore.getInstance().clear();
            PictureTaker.getInstance().configureFromConfig();
            CameraItem.allowUploading = Camerapture.CONFIG_MANAGER.getConfig().server.allowUploading;
        });
    }
}
