package me.chrr.camerapture.fabric;

import io.netty.buffer.ByteBuf;
import me.chrr.camerapture.net.NetCodec;
import me.chrr.camerapture.net.NetworkAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/// Fabric implementation of {@link NetworkAdapter}. This uses the networking API
/// as provided by Fabric API. We need to keep track of handlers per packet, as
/// you can't have multiple listeners for a single packet usually.
public class FabricNetworkAdapter implements NetworkAdapter {
    private final Map<Class<?>, ServerPacketType<?>> serverPackets = new HashMap<>();
    private final Map<Class<?>, ClientPacketType<?>> clientPackets = new HashMap<>();

    public <P> void registerServerBound(Class<P> clazz, NetCodec<P> netCodec) {
        ClientPacketType<P> type = new ClientPacketType<>(netCodec, new ArrayList<>());
        this.clientPackets.put(clazz, type);

        StreamCodec<ByteBuf, P> codec = ByteBufCodecs.fromCodec(netCodec.codec());
        PayloadTypeRegistry.playC2S().register(
                new CustomPacketPayload.Type<PacketPayload<P>>(netCodec.id()),
                StreamCodec.composite(codec, PacketPayload::packet, p -> new PacketPayload<>(netCodec.id(), p))
        );

        ServerPlayNetworking.registerGlobalReceiver(
                new CustomPacketPayload.Type<PacketPayload<P>>(netCodec.id()),
                (payload, context) -> type.handlers().forEach(handler -> handler.accept(payload.packet, context.player()))
        );
    }

    public <P> void registerClientBound(Class<P> clazz, NetCodec<P> netCodec) {
        ServerPacketType<P> type = new ServerPacketType<>(netCodec, new ArrayList<>());
        this.serverPackets.put(clazz, type);

        StreamCodec<io.netty.buffer.ByteBuf, P> codec = ByteBufCodecs.fromCodec(netCodec.codec());
        PayloadTypeRegistry.playS2C().register(
                new CustomPacketPayload.Type<PacketPayload<P>>(netCodec.id()),
                StreamCodec.composite(codec, PacketPayload::packet, p -> new PacketPayload<>(netCodec.id(), p))
        );

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(
                    new CustomPacketPayload.Type<PacketPayload<P>>(netCodec.id()),
                    (payload, context) -> type.handlers().forEach(handler -> handler.accept(payload.packet))
            );
        }
    }

    @Override
    public <P> void sendToClient(ServerPlayer player, P packet) {
        @SuppressWarnings("unchecked") ServerPacketType<P> type = (ServerPacketType<P>) getServerPacketType(packet.getClass());
        ServerPlayNetworking.send(player, new PacketPayload<>(type.netCodec().id(), packet));
    }

    @Override
    public <P> void onReceiveFromClient(Class<P> clazz, BiConsumer<P, ServerPlayer> handler) {
        this.getClientPacketType(clazz).handlers().add(handler);
    }

    @Override
    public <P> void sendToServer(P packet) {
        @SuppressWarnings("unchecked") ClientPacketType<P> type = (ClientPacketType<P>) getClientPacketType(packet.getClass());
        ClientPlayNetworking.send(new PacketPayload<>(type.netCodec().id(), packet));
    }

    @Override
    public <P> void onReceiveFromServer(Class<P> clazz, Consumer<P> handler) {
        this.getServerPacketType(clazz).handlers().add(handler);
    }

    private <P> ServerPacketType<P> getServerPacketType(Class<P> clazz) {
        @SuppressWarnings("unchecked") ServerPacketType<P> type = (ServerPacketType<P>) serverPackets.get(clazz);
        return type;
    }

    private <P> ClientPacketType<P> getClientPacketType(Class<P> clazz) {
        @SuppressWarnings("unchecked") ClientPacketType<P> type = (ClientPacketType<P>) clientPackets.get(clazz);
        return type;
    }

    /// This is the actual packet that's sent over the network. It contains the ID
    /// of the packet, along with the packet itself.
    private record PacketPayload<P>(Identifier id, P packet) implements CustomPacketPayload {
        @Override
        public Type<PacketPayload<P>> type() {
            return new Type<>(id);
        }
    }

    /// Record used for tracking the ID and handlers of a clientbound packet.
    private record ClientPacketType<P>(NetCodec<P> netCodec, List<BiConsumer<P, ServerPlayer>> handlers) {
    }

    /// Record used for tracking the ID and handlers of a serverbound packet.
    private record ServerPacketType<P>(NetCodec<P> netCodec, List<Consumer<P>> handlers) {
    }
}
