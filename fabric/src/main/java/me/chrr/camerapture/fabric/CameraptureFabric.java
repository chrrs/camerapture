package me.chrr.camerapture.fabric;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.DownloadQueue;
import me.chrr.camerapture.config.Config;
import me.chrr.camerapture.config.SyncedConfig;
import me.chrr.camerapture.block.PictureFrameBlock;
import me.chrr.camerapture.block.PictureFrameBlockEntity;
import me.chrr.camerapture.item.AlbumItem;
import me.chrr.camerapture.item.CameraItem;
import me.chrr.camerapture.item.PictureItem;
import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.net.clientbound.SyncConfigPacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import me.chrr.tapestry.gradle.annotation.FabricEntrypoint;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

@FabricEntrypoint("main")
public class CameraptureFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Camerapture.CONFIG_MANAGER.load();

        this.registerContent();
        this.registerPackets();
        this.registerEvents();

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("distantdecorations")) {
            me.chrr.camerapture.compat.distantdecorations.CameraptureDistantDecorationProvider.init();
        }
    }

    public void registerContent() {
        // Camera
        Registry.register(BuiltInRegistries.ITEM, CameraItem.KEY, Camerapture.CAMERA);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> content.accept(Camerapture.CAMERA));
        Registry.register(BuiltInRegistries.SOUND_EVENT, Camerapture.CAMERA_SHUTTER.location(), Camerapture.CAMERA_SHUTTER);

        Registry.register(BuiltInRegistries.CUSTOM_STAT, "pictures_taken", Camerapture.PICTURES_TAKEN);
        Stats.CUSTOM.get(Camerapture.PICTURES_TAKEN, StatFormatter.DEFAULT);

        // Picture
        Registry.register(BuiltInRegistries.ITEM, PictureItem.KEY, Camerapture.PICTURE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Camerapture.id("picture_cloning"), Camerapture.PICTURE_CLONING);

        // Album
        Registry.register(BuiltInRegistries.ITEM, AlbumItem.KEY, Camerapture.ALBUM);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> content.accept(Camerapture.ALBUM));
        Registry.register(BuiltInRegistries.MENU, Camerapture.id("album"), Camerapture.ALBUM_SCREEN_HANDLER);
        Registry.register(BuiltInRegistries.MENU, Camerapture.id("album_lectern"), Camerapture.ALBUM_LECTERN_SCREEN_HANDLER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Camerapture.id("album_cloning"), Camerapture.ALBUM_CLONING);

        // Picture Frame Block
        Registry.register(BuiltInRegistries.BLOCK, PictureFrameBlock.KEY, Camerapture.PICTURE_FRAME_BLOCK);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, PictureFrameBlockEntity.KEY, Camerapture.PICTURE_FRAME_BLOCK_ENTITY);
        Registry.register(BuiltInRegistries.MENU, Camerapture.id("picture_frame"), Camerapture.PICTURE_FRAME_SCREEN_HANDLER);

        // Data components
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Camerapture.id("picture_data"), Camerapture.PICTURE_DATA);
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Camerapture.id("camera_active"), Camerapture.CAMERA_ACTIVE);
    }

    public void registerPackets() {
        FabricNetworkAdapter networkAdapter = (FabricNetworkAdapter) Camerapture.NETWORK;

        networkAdapter.registerServerBound(NewPicturePacket.class, NewPicturePacket.NET_CODEC);
        networkAdapter.registerServerBound(RequestDownloadPacket.class, RequestDownloadPacket.NET_CODEC);
        networkAdapter.registerServerBound(UploadPartialPicturePacket.class, UploadPartialPicturePacket.NET_CODEC);
        networkAdapter.registerClientBound(PictureErrorPacket.class, PictureErrorPacket.NET_CODEC);
        networkAdapter.registerClientBound(RequestUploadPacket.class, RequestUploadPacket.NET_CODEC);
        networkAdapter.registerClientBound(SyncConfigPacket.class, SyncConfigPacket.NET_CODEC);
        networkAdapter.registerClientBound(DownloadPartialPicturePacket.class, DownloadPartialPicturePacket.NET_CODEC);

        Camerapture.registerPacketHandlers();
    }

    public void registerEvents() {
        // When a player joins, we send them our config.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Config config = Camerapture.CONFIG_MANAGER.getConfig();
            Camerapture.NETWORK.sendToClient(handler.player, new SyncConfigPacket(SyncedConfig.fromServerConfig(config.server)));
        });

        // Run the download queue while the server is started.
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                DownloadQueue.getInstance().start(Camerapture.CONFIG_MANAGER.getConfig().server.msPerPicture));
        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                DownloadQueue.getInstance().stop());
    }
}
