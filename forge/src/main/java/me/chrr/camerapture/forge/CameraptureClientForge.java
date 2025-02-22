package me.chrr.camerapture.forge;

import com.luciad.imageio.webp.WebPImageReaderSpi;
import com.luciad.imageio.webp.WebPImageWriterSpi;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.compat.ClothConfigScreenFactory;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.gui.*;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.PictureFrameEntityRenderer;
import me.chrr.camerapture.render.PictureItemRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import javax.imageio.spi.IIORegistry;
import java.util.List;

public class CameraptureClientForge {
    public CameraptureClientForge(IEventBus modBus) {
        modBus.register(this);
        MinecraftForge.EVENT_BUS.register(new ClientEvents());

        if (ModList.get().isLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((client, parent) -> ClothConfigScreenFactory.create(parent)));
        }
    }

    @SubscribeEvent
    public void clientSetup(FMLClientSetupEvent event) {
        CameraptureClient.init();
        CameraptureClient.registerPacketHandlers();

        // FIXME: Figure out why this is necessary. This isn't needed in built versions, however
        //        the development environment doesn't detect the WebP-ImageIO services.
        IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
        IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageWriterSpi());

        event.enqueueWork(() -> {
            // Picture
            ModelPredicateProviderRegistry.register(Camerapture.PICTURE, Camerapture.id("should_render_picture"),
                    (stack, world, entity, seed) -> PictureItemRenderer.canRender(stack) ? 1f : 0f);

            // Handled screens
            HandledScreens.register(Camerapture.PICTURE_FRAME_SCREEN_HANDLER, PictureFrameScreen::new);
            HandledScreens.register(Camerapture.ALBUM_SCREEN_HANDLER, AlbumScreen::new);
            HandledScreens.register(Camerapture.ALBUM_LECTERN_SCREEN_HANDLER, AlbumLecternScreen::new);
        });
    }

    @SubscribeEvent
    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Camerapture.PICTURE_FRAME, PictureFrameEntityRenderer::new);
    }

    private static class ClientEvents {
        /// When attacking with an active camera, we want to take a picture.
        @SubscribeEvent
        public void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isAttack()) {
                return;
            }

            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) {
                return;
            }

            CameraItem.HeldCamera camera = CameraItem.find(player, true);
            if (camera == null) {
                return;
            }

            if (CameraItem.canTakePicture(player)) {
                PictureTaker.getInstance().takePicture();
            }

            event.setSwingHand(false);
            event.setCanceled(true);
        }

        /// Right-clicking on certain items should open client-side GUI's.
        @SubscribeEvent
        public ActionResult onUseItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getSide() != LogicalSide.CLIENT) {
                return ActionResult.PASS;
            }

            ItemStack stack = event.getItemStack();
            PlayerEntity player = event.getEntity();
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != player) {
                return ActionResult.PASS;
            }

            if (stack.isOf(Camerapture.PICTURE)) {
                // Right-clicking a picture item should open the picture screen.
                if (PictureItem.getPictureData(stack) != null) {
                    client.executeSync(() -> client.setScreen(new PictureScreen(List.of(stack))));
                    return ActionResult.SUCCESS;
                }
            } else if (stack.isOf(Camerapture.ALBUM) && !player.isSneaking()) {
                // Right-clicking the album should open the gallery screen.
                List<ItemStack> pictures = AlbumItem.getPictures(stack);
                if (!pictures.isEmpty()) {
                    client.executeSync(() -> client.setScreen(new PictureScreen(pictures)));
                    return ActionResult.SUCCESS;
                }
            } else if (CameraptureClient.syncedConfig.allowUploading()
                    && player.isSneaking()
                    && stack.isOf(Camerapture.CAMERA)
                    && !CameraItem.isActive(stack)
                    && !player.getItemCooldownManager().isCoolingDown(Camerapture.CAMERA)) {
                // Shift-right clicking the camera should open the upload screen.
                client.executeSync(() -> client.setScreen(new UploadScreen()));
                return ActionResult.SUCCESS;
            }

            return ActionResult.PASS;
        }

        /// We need to notify the picture taker when the render tick ends.
        @SubscribeEvent
        public void onRenderTickEnd(TickEvent.RenderTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                PictureTaker.getInstance().renderTickEnd();
            }
        }

        /// Clear cache and reset the picture taker configuration when logging out of a world.
        @SubscribeEvent
        public void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            ClientPictureStore.getInstance().clear();
            CameraptureClient.syncedConfig = SyncedConfig.fromServerConfig(Camerapture.CONFIG_MANAGER.getConfig().server);
        }

        /// Hide the hand when the player is holding an active camera.
        @SubscribeEvent
        public void onRenderHand(RenderHandEvent event) {
            CameraItem.HeldCamera camera = CameraItem.find(MinecraftClient.getInstance().player, true);
            if (camera != null) {
                event.setCanceled(true);
            }
        }

        /// Hide the GUI and draw the camera overlay and viewfinder
        /// when the player is holding an active camera.
        @SubscribeEvent
        public void onRenderGui(RenderGuiEvent.Pre event) {
            CameraItem.HeldCamera camera = CameraItem.find(MinecraftClient.getInstance().player, true);
            if (camera != null) {
                event.setCanceled(true);
            } else {
                PictureTaker.getInstance().zoomLevel = CameraptureClient.MIN_ZOOM;
                return;
            }

            if (!MinecraftClient.getInstance().options.hudHidden) {
                CameraViewFinder.drawCameraViewFinder(event.getGuiGraphics(), MinecraftClient.getInstance().textRenderer);
            }
        }

        /// If we have an active camera, scroll to zoom instead.
        @SubscribeEvent
        public void onScroll(InputEvent.MouseScrollingEvent event) {
            if (CameraItem.find(MinecraftClient.getInstance().player, true) != null) {
                PictureTaker.getInstance().zoom((float) (event.getScrollDelta() / 4f));
                event.setCanceled(true);
            }
        }

        /// Apply the camera zoom FOV if we have an active camera.
        @SubscribeEvent
        public void onFovModifier(ComputeFovModifierEvent event) {
            if (CameraItem.find(MinecraftClient.getInstance().player, true) != null) {
                event.setNewFovModifier(PictureTaker.getInstance().getFovModifier());
            }
        }
    }
}
