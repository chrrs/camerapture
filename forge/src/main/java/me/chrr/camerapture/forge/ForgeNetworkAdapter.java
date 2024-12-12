package me.chrr.camerapture.forge;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import me.chrr.camerapture.net.NetworkAdapter;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/// Forge implementation of {@link NetworkAdapter}. This uses the networking API
/// as provided by Forge. We need to keep track of handlers per packet, as
/// you can't have multiple listeners for a single packet usually.
public class ForgeNetworkAdapter implements NetworkAdapter {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            Camerapture.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    private final Map<Class<?>, ServerPacketType<?>> serverPackets = new HashMap<>();
    private final Map<Class<?>, ClientPacketType<?>> clientPackets = new HashMap<>();

    public <P> void registerServerBound(Class<P> clazz, NetCodec<P> netCodec) {
        ClientPacketType<P> type = new ClientPacketType<>(netCodec, new ArrayList<>());
        this.clientPackets.put(clazz, type);

        CHANNEL.registerMessage(nextId++, clazz,
                (packet, buf) -> encodePacket(packet, buf, type.netCodec()),
                (buf) -> decodePacket(buf, type.netCodec()),
                (packet, context) -> {
                    if (packet != null) {
                        type.handlers().forEach(handler -> handler.accept(packet, context.get().getSender()));
                    }

                    context.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public <P> void registerClientBound(Class<P> clazz, NetCodec<P> netCodec) {
        ServerPacketType<P> type = new ServerPacketType<>(netCodec, new ArrayList<>());
        this.serverPackets.put(clazz, type);

        CHANNEL.registerMessage(nextId++, clazz,
                (packet, buf) -> encodePacket(packet, buf, type.netCodec()),
                (buf) -> decodePacket(buf, type.netCodec()),
                (packet, context) -> {
                    if (packet != null) {
                        type.handlers().forEach(handler -> handler.accept(packet));
                    }

                    context.get().setPacketHandled(true);
                },
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    @Override
    public <P> void sendToClient(ServerPlayerEntity player, P packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    @Override
    public <P> void onReceiveFromClient(Class<P> clazz, BiConsumer<P, ServerPlayerEntity> handler) {
        this.getClientPacketType(clazz).handlers().add(handler);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public <P> void sendToServer(P packet) {
        CHANNEL.sendToServer(packet);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public <P> void onReceiveFromServer(Class<P> clazz, Consumer<P> handler) {
        this.getServerPacketType(clazz).handlers().add(handler);
    }

    private static <P> void encodePacket(P packet, PacketByteBuf buf, NetCodec<P> netCodec) {
        MapCodec<P> dataCodec = netCodec.codec().fieldOf("data");
        DataResult<NbtElement> result = dataCodec.encoder().encodeStart(NbtOps.INSTANCE, packet);
        buf.writeNbt((NbtCompound) result.get().left().orElseThrow());
    }

    private static <P> @Nullable P decodePacket(PacketByteBuf buf, NetCodec<P> netCodec) {
        MapCodec<P> dataCodec = netCodec.codec().fieldOf("data");
        DataResult<Pair<P, NbtElement>> result = dataCodec.decoder().decode(NbtOps.INSTANCE, buf.readNbt());

        if (result.error().isPresent()) {
            Camerapture.LOGGER.error("failed to decode packet: {}", result.error().get().message());
            return null;
        }

        return result.result().orElseThrow().getFirst();
    }

    private <P> ServerPacketType<P> getServerPacketType(Class<P> clazz) {
        @SuppressWarnings("unchecked") ServerPacketType<P> type = (ServerPacketType<P>) serverPackets.get(clazz);
        return type;
    }

    private <P> ClientPacketType<P> getClientPacketType(Class<P> clazz) {
        @SuppressWarnings("unchecked") ClientPacketType<P> type = (ClientPacketType<P>) clientPackets.get(clazz);
        return type;
    }

    /// Record used for tracking the ID and handlers of a clientbound packet.
    private record ClientPacketType<P>(NetCodec<P> netCodec, List<BiConsumer<P, ServerPlayerEntity>> handlers) {
    }

    /// Record used for tracking the ID and handlers of a serverbound packet.
    private record ServerPacketType<P>(NetCodec<P> netCodec, List<Consumer<P>> handlers) {
    }
}
