package me.chrr.camerapture.neoforge;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.CameraptureClient;
import me.chrr.camerapture.compat.ClothConfigScreenFactory;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.gui.*;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.picture.ClientPictureStore;
import me.chrr.camerapture.picture.PictureTaker;
import me.chrr.camerapture.render.PictureItemRenderer;
import me.chrr.camerapture.render.ShouldRenderPicture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@Mod(value = Camerapture.MOD_ID, dist = Dist.CLIENT)
public class CameraptureClientNeoForge {
    public CameraptureClientNeoForge(ModContainer mod) {
        Objects.requireNonNull(mod.getEventBus()).register(this);
        NeoForge.EVENT_BUS.register(new ClientEvents());

        if (ModList.get().isLoaded("cloth_config")) {
            mod.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> ClothConfigScreenFactory.create(parent));
        }
    }

    @SubscribeEvent
    public void setup(FMLClientSetupEvent event) {
        CameraptureClient.init();
    }

    @SubscribeEvent
    public void registerHandledScreens(RegisterMenuScreensEvent event) {
        event.register(Camerapture.PICTURE_FRAME_SCREEN_HANDLER, PictureFrameScreen::new);
        event.register(Camerapture.ALBUM_SCREEN_HANDLER, AlbumScreen::new);
        event.register(Camerapture.ALBUM_LECTERN_SCREEN_HANDLER, AlbumLecternScreen::new);
    }

    @SubscribeEvent
    public void registerPackets(RegisterPayloadHandlersEvent event) {
        CameraptureClient.registerPacketHandlers();
    }

    @SubscribeEvent
    public void registerItemModelConditions(RegisterConditionalItemModelPropertyEvent event) {
        event.register(Camerapture.id("should_render_picture"), ShouldRenderPicture.MAP_CODEC);
    }

    @SubscribeEvent
    public void registerItemRenderers(RegisterSpecialModelRendererEvent event) {
        event.register(Camerapture.id("picture"), PictureItemRenderer.Unbaked.MAP_CODEC);
    }

    @SubscribeEvent
    public void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(Camerapture.PICTURE_FRAME_BLOCK_ENTITY, NeoPictureFrameBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public void registerClientExtensions(RegisterClientExtensionsEvent event) {
        // If we're holding a camera, we want to have the arm pose as if we're
        // charging a bow and arrow, so we hold the camera up.
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(@NotNull LivingEntity entity, @NotNull InteractionHand hand, @NotNull ItemStack stack) {
                return CameraItem.isActive(stack) ? HumanoidModel.ArmPose.BOW_AND_ARROW : null;
            }
        }, Camerapture.CAMERA);
    }

    private static class ClientEvents {
        /// When attacking with an active camera, we want to take a picture.
        @SubscribeEvent
        public void onAttack(InputEvent.InteractionKeyMappingTriggered event) {
            if (!event.isAttack()) {
                return;
            }

            LocalPlayer player = Minecraft.getInstance().player;
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
        public InteractionResult onUseItem(PlayerInteractEvent.RightClickItem event) {
            if (event.getSide() != LogicalSide.CLIENT) {
                return InteractionResult.PASS;
            }

            ItemStack stack = event.getItemStack();
            Player player = event.getEntity();
            return CameraptureClient.onUseItem(player, stack);
        }

        /// We need to notify the picture taker when the render tick ends.
        @SubscribeEvent
        public void onRenderTickEnd(RenderFrameEvent.Post event) {
            PictureTaker.getInstance().renderTickEnd();
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
            CameraItem.HeldCamera camera = CameraItem.find(Minecraft.getInstance().player, true);
            if (camera != null) {
                event.setCanceled(true);
            }
        }

        /// Hide the GUI and draw the camera overlay and viewfinder
        /// when the player is holding an active camera.
        @SubscribeEvent
        public void onRenderGui(RenderGuiLayerEvent.Pre event) {
            CameraItem.HeldCamera camera = CameraItem.find(Minecraft.getInstance().player, true);
            if (camera != null) {
                event.setCanceled(true);
            } else {
                PictureTaker.getInstance().zoomLevel = CameraptureClient.MIN_ZOOM;
                return;
            }

            if (event.getName() == VanillaGuiLayers.CROSSHAIR && !Minecraft.getInstance().gui.hud.isHidden()) {
                CameraViewFinder.drawCameraViewFinder(event.getGuiGraphics(), Minecraft.getInstance().font);
            }
        }

        /// If we have an active camera, scroll to zoom instead.
        @SubscribeEvent
        public void onScroll(InputEvent.MouseScrollingEvent event) {
            if (CameraItem.find(Minecraft.getInstance().player, true) != null) {
                PictureTaker.getInstance().zoom((float) (event.getScrollDeltaY() / 4f));
                event.setCanceled(true);
            }
        }

        /// Apply the camera zoom FOV if we have an active camera. This is registered on low priority, to make sure
        /// we're applying the FOV modifier last.
        @SubscribeEvent(priority = EventPriority.LOW)
        public void onFovModifier(ComputeFovModifierEvent event) {
            if (CameraItem.find(Minecraft.getInstance().player, true) != null) {
                event.setNewFovModifier(PictureTaker.getInstance().getFovModifier());
            }
        }

        /// Process any received pictures once per tick.
        @SubscribeEvent
        public void onClientTick(ClientTickEvent.Pre event) {
            ClientPictureStore.getInstance().processQueue();
        }
    }
}
