package me.chrr.camerapture.forge;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Camerapture.MOD_ID)
public class CameraptureForge {
    public CameraptureForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        modBus.register(this);
        MinecraftForge.EVENT_BUS.register(new ServerEvents());

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> new CameraptureClientForge(modBus));
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

        this.registerPackets();
    }

    public void registerPackets() {
        ForgeNetworkAdapter networkAdapter = (ForgeNetworkAdapter) Camerapture.NETWORK;

        networkAdapter.registerServerBound(NewPicturePacket.class, NewPicturePacket.NET_CODEC);
        networkAdapter.registerServerBound(RequestDownloadPacket.class, RequestDownloadPacket.NET_CODEC);
        networkAdapter.registerServerBound(UploadPartialPicturePacket.class, UploadPartialPicturePacket.NET_CODEC);
        networkAdapter.registerClientBound(PictureErrorPacket.class, PictureErrorPacket.NET_CODEC);
        networkAdapter.registerClientBound(RequestUploadPacket.class, RequestUploadPacket.NET_CODEC);
        networkAdapter.registerClientBound(SyncConfigPacket.class, SyncConfigPacket.NET_CODEC);
        networkAdapter.registerClientBound(DownloadPartialPicturePacket.class, DownloadPartialPicturePacket.NET_CODEC);

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
