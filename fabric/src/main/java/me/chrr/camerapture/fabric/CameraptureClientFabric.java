package me.chrr.camerapture.fabric;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.gui.*;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.PictureFrameBlockEntityRenderer;
import me.chrr.camerapture.render.PictureFrameEntityRenderer;
import me.chrr.camerapture.render.PictureItemRenderer;
import me.chrr.camerapture.render.ShouldRenderPicture;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;

@FabricEntrypoint("client")
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
        ConditionalItemModelProperties.ID_MAPPER.put(Camerapture.id("should_render_picture"), ShouldRenderPicture.MAP_CODEC);
        SpecialModelRenderers.ID_MAPPER.put(Camerapture.id("picture"), PictureItemRenderer.Unbaked.MAP_CODEC);

        // Picture Frame
        BlockEntityRenderers.register(Camerapture.PICTURE_FRAME_BLOCK_ENTITY, PictureFrameBlockEntityRenderer::new);
        EntityRenderers.register(Camerapture.PICTURE_FRAME, PictureFrameEntityRenderer::new);
        MenuScreens.register(Camerapture.PICTURE_FRAME_SCREEN_HANDLER, PictureFrameScreen::new);

        // Album
        MenuScreens.register(Camerapture.ALBUM_SCREEN_HANDLER, AlbumScreen::new);
        MenuScreens.register(Camerapture.ALBUM_LECTERN_SCREEN_HANDLER, AlbumLecternScreen::new);
    }

    public void registerClientEvents() {
        // When attacking with an active camera, we want to take a picture.
        ClientPreAttackCallback.EVENT.register((minecraft, player, clickCount) -> {
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
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!level.isClientSide()) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            return CameraptureClient.onUseItem(player, stack);
        });

        // Clear cache and reset the picture taker configuration when logging out of a world.
        ClientPlayConnectionEvents.DISCONNECT.register((listener, minecraft) -> {
            ClientPictureStore.getInstance().clear();
            CameraptureClient.syncedConfig = SyncedConfig.fromServerConfig(Camerapture.CONFIG_MANAGER.getConfig().server);
        });

        // Process any received pictures once per tick.
        ClientTickEvents.START_CLIENT_TICK.register((minecraft) ->
                ClientPictureStore.getInstance().processQueue());
    }
}
