package me.chrr.camerapture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import me.chrr.camerapture.config.ConfigManager;
import me.chrr.camerapture.gui.AlbumLecternMenu;
import me.chrr.camerapture.gui.AlbumMenu;
import me.chrr.camerapture.gui.PictureFrameMenu;
import me.chrr.camerapture.item.*;
import me.chrr.camerapture.net.NetworkAdapter;
import me.chrr.camerapture.net.clientbound.PictureErrorPacket;
import me.chrr.camerapture.net.clientbound.RequestUploadPacket;
import me.chrr.camerapture.net.serverbound.NewPicturePacket;
import me.chrr.camerapture.net.serverbound.RequestDownloadPacket;
import me.chrr.camerapture.net.serverbound.UploadPartialPicturePacket;
import me.chrr.camerapture.block.PictureFrameBlock;
import me.chrr.camerapture.block.PictureFrameBlockEntity;
import me.chrr.camerapture.picture.ServerPictureStore;
import me.chrr.camerapture.picture.StoredPicture;
import me.chrr.tapestry.base.Tapestry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Camerapture {
    public static final String MOD_ID = "camerapture";
    public static final Logger LOGGER = LogManager.getLogger("Camerapture");

    public static final Executor EXECUTOR = Executors.newCachedThreadPool();
    public static final ConfigManager CONFIG_MANAGER = new ConfigManager();

    public static final PlatformAdapter PLATFORM = Tapestry.implementation(id("platform_adapter"));
    public static final NetworkAdapter NETWORK = Tapestry.implementation(id("network_adapter"));

    // Server-bound packets have a way lower limit on size.
    public static final int CLIENT_SECTION_SIZE = 30_000;
    public static final int SERVER_SECTION_SIZE = 1_000_000;

    // Camera
    public static Item CAMERA = new CameraItem();
    public static final SoundEvent CAMERA_SHUTTER = SoundEvent.createVariableRangeEvent(id("camera_shutter"));
    public static final Identifier PICTURES_TAKEN = id("pictures_taken");

    // Picture
    public static Item PICTURE = new PictureItem();
    public static final RecipeSerializer<PictureCloningRecipe> PICTURE_CLONING = new RecipeSerializer<>(
            MapCodec.unit(PictureCloningRecipe.INSTANCE), StreamCodec.unit(PictureCloningRecipe.INSTANCE));

    // Album
    public static final Item ALBUM = new AlbumItem();
    public static final MenuType<AlbumMenu> ALBUM_SCREEN_HANDLER = new MenuType<>(AlbumMenu::new, FeatureFlagSet.of());
    public static final MenuType<AlbumLecternMenu> ALBUM_LECTERN_SCREEN_HANDLER =
            new MenuType<>((containerId, playerInventory) -> new AlbumLecternMenu(containerId), FeatureFlagSet.of());
    public static final RecipeSerializer<AlbumCloningRecipe> ALBUM_CLONING = new RecipeSerializer<>(
            MapCodec.unit(AlbumCloningRecipe.INSTANCE), StreamCodec.unit(AlbumCloningRecipe.INSTANCE));

    // Picture Frame Block
    public static final Block PICTURE_FRAME_BLOCK = new PictureFrameBlock();
    public static final BlockEntityType<PictureFrameBlockEntity> PICTURE_FRAME_BLOCK_ENTITY =
            new BlockEntityType<>(PictureFrameBlockEntity::new, java.util.Set.of(PICTURE_FRAME_BLOCK));
    public static final MenuType<PictureFrameMenu> PICTURE_FRAME_SCREEN_HANDLER =
            new MenuType<>((containerId, pi) -> new PictureFrameMenu(containerId), FeatureFlagSet.of());

    // Data Components
    public static final DataComponentType<PictureItem.PictureData> PICTURE_DATA = DataComponentType.<PictureItem.PictureData>builder()
            .persistent(PictureItem.PictureData.CODEC).networkSynchronized(PictureItem.PictureData.PACKET_CODEC)
            .build();
    public static final DataComponentType<Boolean> CAMERA_ACTIVE = DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
            .build();

    public static void registerPacketHandlers() {
        // Client requests to take / upload a picture
        NETWORK.onReceiveFromClient(NewPicturePacket.class, (packet, player) -> {
            CameraItem.HeldCamera camera = CameraItem.find(player, false);
            if (camera == null) {
                return;
            }

            // If the player is in creative mode, skip taking any paper.
            if (!player.isCreative()) {
                if (ContainerHelper.clearOrCountMatchingItems(player.getInventory(), (stack) -> stack.is(Items.PAPER), 1, false) != 1) {
                    return;
                }
            }

            // We don't want to play the sound when the player is uploading a picture, only when it's being taken.
            if (CameraItem.isActive(camera.stack())) {
                //noinspection resource: we don't want to close the level.
                player.level().playSound(null, player, CAMERA_SHUTTER, SoundSource.PLAYERS, 1f, 1f);
            }

            CameraItem.setActive(camera.stack(), false);
            player.getCooldowns().addCooldown(camera.stack(), 20 * 3);
            player.swing(camera.hand(), true);

            player.awardStat(PICTURES_TAKEN);

            UUID id = ServerPictureStore.getInstance().reserveId();
            NETWORK.sendToClient(player, new RequestUploadPacket(id));
        });

        // Client sends back a picture following a take-picture request
        Map<UUID, ByteCollector> collectors = new ConcurrentHashMap<>();
        NETWORK.onReceiveFromClient(UploadPartialPicturePacket.class, (packet, player) -> {
            if (!ServerPictureStore.getInstance().isReserved(packet.uuid())) {
                LOGGER.error("{} tried to send a byte section for an unreserved UUID", player.getName().toString());
                return;
            }

            if (packet.bytesLeft() > CONFIG_MANAGER.getConfig().server.maxImageBytes) {
                LOGGER.error("{} sent a picture exceeding the size limit", player.getName().getString());
                collectors.remove(packet.uuid());
                ServerPictureStore.getInstance().unreserveId(packet.uuid());
            }

            ByteCollector collector;
            synchronized (collectors) {
                collector = collectors.computeIfAbsent(packet.uuid(), (uuid) -> new ByteCollector((bytes) -> {
                    collectors.remove(uuid);
                    EXECUTOR.execute(() -> {
                        try {
                            MinecraftServer server = player.server;

                            ServerPictureStore.getInstance().put(server, uuid, new StoredPicture(bytes));
                            ItemStack picture = PictureItem.create(player.getName().getString(), uuid);

                            // We have to do this on a separate thread, because it might spawn an item entity.
                            server.execute(() -> player.getInventory().placeItemBackInInventory(picture));
                        } catch (Exception e) {
                            LOGGER.error("failed to save picture from {}", player.getName().getString(), e);
                            player.sendSystemMessage(Component.translatable("text.camerapture.picture_failed").withStyle(ChatFormatting.RED), false);
                        }
                    });
                }));
            }

            synchronized (collector) {
                if (!collector.push(packet.bytes(), packet.bytesLeft())) {
                    LOGGER.error("{} sent a malformed byte section", player.getName().getString());
                    collectors.remove(packet.uuid());
                    ServerPictureStore.getInstance().unreserveId(packet.uuid());
                }

                if (collector.getCurrentLength() > CONFIG_MANAGER.getConfig().server.maxImageBytes) {
                    LOGGER.error("{} sent a picture exceeding the size limit", player.getName().getString());
                    collectors.remove(packet.uuid());
                    ServerPictureStore.getInstance().unreserveId(packet.uuid());
                }
            }
        });

        // Client requests a picture with a certain UUID
        NETWORK.onReceiveFromClient(RequestDownloadPacket.class, (packet, player) -> {
            try {
                StoredPicture picture = ServerPictureStore.getInstance().get(player.server, packet.uuid());

                if (picture == null) {
                    LOGGER.warn("{} requested a picture with an unknown UUID", player.getName().getString());
                    NETWORK.sendToClient(player, new PictureErrorPacket(packet.uuid()));
                    return;
                }

                DownloadQueue.getInstance().send(player, packet.uuid(), picture);
            } catch (Exception e) {
                LOGGER.error("failed to load picture for {}", player.getName().getString(), e);
                NETWORK.sendToClient(player, new PictureErrorPacket(packet.uuid()));
            }
        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("camerapture", path);
    }
}
