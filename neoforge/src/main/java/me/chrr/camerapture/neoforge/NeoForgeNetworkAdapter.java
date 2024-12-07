package me.chrr.camerapture.neoforge;

import io.netty.buffer.ByteBuf;
import me.chrr.camerapture.net.NetCodec;
import me.chrr.camerapture.net.NetworkAdapter;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/// NeoForge implementation of {@link NetworkAdapter}. This uses the networking API
/// as provided by NeoForge. We need to keep track of handlers per packet, as
/// you can't have multiple listeners for a single packet usually.
public class NeoForgeNetworkAdapter implements NetworkAdapter {
    private final Map<Class<?>, ServerPacketType<?>> serverPackets = new HashMap<>();
    private final Map<Class<?>, ClientPacketType<?>> clientPackets = new HashMap<>();

    public <P> void registerServerBound(PayloadRegistrar registrar, Class<P> clazz, NetCodec<P> netCodec) {
        ClientPacketType<P> type = new ClientPacketType<>(netCodec, new ArrayList<>());
        this.clientPackets.put(clazz, type);

        PacketCodec<ByteBuf, P> codec = PacketCodecs.codec(netCodec.codec());
        registrar.playToServer(
                new CustomPayload.Id<PacketPayload<P>>(netCodec.id()),
                PacketCodec.tuple(codec, PacketPayload::packet, p -> new PacketPayload<>(netCodec.id(), p)),
                (payload, context) -> type.handlers().forEach(handler -> handler.accept(payload.packet, (ServerPlayerEntity) context.player()))
        );
    }

    public <P> void registerClientBound(PayloadRegistrar registrar, Class<P> clazz, NetCodec<P> netCodec) {
        ServerPacketType<P> type = new ServerPacketType<>(netCodec, new ArrayList<>());
        this.serverPackets.put(clazz, type);

        PacketCodec<ByteBuf, P> codec = PacketCodecs.codec(netCodec.codec());
        registrar.playToClient(
                new CustomPayload.Id<PacketPayload<P>>(netCodec.id()),
                PacketCodec.tuple(codec, PacketPayload::packet, p -> new PacketPayload<>(netCodec.id(), p)),
                (payload, context) -> type.handlers().forEach(handler -> handler.accept(payload.packet))
        );
    }

    @Override
    public <P> void sendToClient(ServerPlayerEntity player, P packet) {
        @SuppressWarnings("unchecked") ServerPacketType<P> type = (ServerPacketType<P>) getServerPacketType(packet.getClass());
        PacketDistributor.sendToPlayer(player, new PacketPayload<>(type.netCodec().id(), packet));
    }

    @Override
    public <P> void onReceiveFromClient(Class<P> clazz, BiConsumer<P, ServerPlayerEntity> handler) {
        this.getClientPacketType(clazz).handlers().add(handler);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public <P> void sendToServer(P packet) {
        @SuppressWarnings("unchecked") ClientPacketType<P> type = (ClientPacketType<P>) getClientPacketType(packet.getClass());
        PacketDistributor.sendToServer(new PacketPayload<>(type.netCodec().id(), packet));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
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
    private record PacketPayload<P>(Identifier id, P packet) implements CustomPayload {
        @Override
        public Id<PacketPayload<P>> getId() {
            return new Id<>(id);
        }
    }

    /// Record used for tracking the ID and handlers of a clientbound packet.
    private record ClientPacketType<P>(NetCodec<P> netCodec, List<BiConsumer<P, ServerPlayerEntity>> handlers) {
    }

    /// Record used for tracking the ID and handlers of a serverbound packet.
    private record ServerPacketType<P>(NetCodec<P> netCodec, List<Consumer<P>> handlers) {
    }
}
