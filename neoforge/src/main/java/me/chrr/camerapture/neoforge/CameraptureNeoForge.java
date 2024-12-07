package me.chrr.camerapture.neoforge;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.DownloadQueue;
import me.chrr.camerapture.config.Config;
import me.chrr.camerapture.entity.PictureFrameEntity;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.net.serverbound.SyncConfigPacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.StatFormatter;
import net.minecraft.stat.Stats;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Camerapture.MOD_ID)
public class CameraptureNeoForge {
    public CameraptureNeoForge(IEventBus modBus) {
        modBus.register(this);
        NeoForge.EVENT_BUS.register(new ServerEvents());
    }

    @SubscribeEvent
    public void registerContent(RegisterEvent event) {
        Camerapture.CONFIG_MANAGER.load();

        // Camera
        event.register(RegistryKeys.ITEM, registry ->
                registry.register(CameraItem.KEY, Camerapture.CAMERA));
        event.register(RegistryKeys.SOUND_EVENT, registry ->
                registry.register(Camerapture.id("camera_shutter"), Camerapture.CAMERA_SHUTTER));

        event.register(RegistryKeys.CUSTOM_STAT, registry -> {
            registry.register(Camerapture.PICTURES_TAKEN, Camerapture.PICTURES_TAKEN);
            Stats.CUSTOM.getOrCreateStat(Camerapture.PICTURES_TAKEN, StatFormatter.DEFAULT);
        });

        // Picture
        event.register(RegistryKeys.ITEM, registry ->
                registry.register(PictureItem.KEY, Camerapture.PICTURE));
        event.register(RegistryKeys.RECIPE_SERIALIZER, registry ->
                registry.register(Camerapture.id("picture_cloning"), Camerapture.PICTURE_CLONING));

        // Album
        event.register(RegistryKeys.ITEM, registry ->
                registry.register(AlbumItem.KEY, Camerapture.ALBUM));
        event.register(RegistryKeys.SCREEN_HANDLER, registry ->
                registry.register(Camerapture.id("album"), Camerapture.ALBUM_SCREEN_HANDLER));
        event.register(RegistryKeys.SCREEN_HANDLER, registry ->
                registry.register(Camerapture.id("album_lectern"), Camerapture.ALBUM_LECTERN_SCREEN_HANDLER));

        // Picture Frame
        event.register(RegistryKeys.ENTITY_TYPE, registry ->
                registry.register(PictureFrameEntity.KEY, Camerapture.PICTURE_FRAME));
        event.register(RegistryKeys.SCREEN_HANDLER, registry ->
                registry.register(Camerapture.id("picture_frame"), Camerapture.PICTURE_FRAME_SCREEN_HANDLER));

        // Data components
        event.register(RegistryKeys.DATA_COMPONENT_TYPE, registry ->
                registry.register(Camerapture.id("picture_data"), Camerapture.PICTURE_DATA));
        event.register(RegistryKeys.DATA_COMPONENT_TYPE, registry ->
                registry.register(Camerapture.id("camera_active"), Camerapture.CAMERA_ACTIVE));
    }

    @SubscribeEvent
    public void registerPackets(RegisterPayloadHandlersEvent event) {
        NeoForgeNetworkAdapter networkAdapter = (NeoForgeNetworkAdapter) Camerapture.NETWORK;
        PayloadRegistrar registrar = event.registrar("1");

        networkAdapter.registerServerBound(registrar, NewPicturePacket.class, NewPicturePacket.NET_CODEC);
        networkAdapter.registerServerBound(registrar, RequestDownloadPacket.class, RequestDownloadPacket.NET_CODEC);
        networkAdapter.registerServerBound(registrar, UploadPartialPicturePacket.class, UploadPartialPicturePacket.NET_CODEC);
        networkAdapter.registerClientBound(registrar, PictureErrorPacket.class, PictureErrorPacket.NET_CODEC);
        networkAdapter.registerClientBound(registrar, RequestUploadPacket.class, RequestUploadPacket.NET_CODEC);
        networkAdapter.registerClientBound(registrar, SyncConfigPacket.class, SyncConfigPacket.NET_CODEC);
        networkAdapter.registerClientBound(registrar, DownloadPartialPicturePacket.class, DownloadPartialPicturePacket.NET_CODEC);

        Camerapture.registerPacketHandlers();
    }

    @SubscribeEvent
    public void fillCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ItemGroups.TOOLS) {
            event.add(Camerapture.CAMERA);
            event.add(Camerapture.ALBUM);
        }
    }

    private static class ServerEvents {
        /// When a player joins, we send them our config.
        @SubscribeEvent
        public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
            Config config = Camerapture.CONFIG_MANAGER.getConfig();
            Camerapture.NETWORK.sendToClient((ServerPlayerEntity) event.getEntity(), new SyncConfigPacket(config.server.maxImageBytes, config.server.maxImageResolution));
        }

        /// When the server is running, we start the timer that sends the pictures.
        @SubscribeEvent
        public void onServerStarted(ServerStartedEvent event) {
            DownloadQueue.getInstance().start(Camerapture.CONFIG_MANAGER.getConfig().server.msPerPicture);
        }

        /// When the server stops, we also stop the timer.
        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {
            DownloadQueue.getInstance().stop();
        }
    }
}
