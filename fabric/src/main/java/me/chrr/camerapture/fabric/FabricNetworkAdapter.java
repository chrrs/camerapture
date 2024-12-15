package me.chrr.camerapture.fabric;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.net.NetCodec;
import me.chrr.camerapture.net.NetworkAdapter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

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

        ServerPlayNetworking.registerGlobalReceiver(netCodec.id(),
                (server, player, networkHandler, buf, sender) -> {
                    P packet = decodePacket(buf, type.netCodec());
                    if (packet != null) {
                        type.handlers().forEach(handler -> handler.accept(packet, player));
                    }
                }
        );
    }

    public <P> void registerClientBound(Class<P> clazz, NetCodec<P> netCodec) {
        ServerPacketType<P> type = new ServerPacketType<>(netCodec, new ArrayList<>());
        this.serverPackets.put(clazz, type);

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.registerGlobalReceiver(type.netCodec().id(),
                    (client, networkHandler, buf, sender) -> {
                        P packet = decodePacket(buf, type.netCodec());
                        if (packet != null) {
                            type.handlers().forEach(handler -> handler.accept(packet));
                        }
                    }
            );
        }
    }

    @Override
    public <P> void sendToClient(ServerPlayerEntity player, P packet) {
        @SuppressWarnings("unchecked") ServerPacketType<P> type = (ServerPacketType<P>) getServerPacketType(packet.getClass());

        PacketByteBuf buf = PacketByteBufs.create();
        encodePacket(packet, buf, type.netCodec());
        ServerPlayNetworking.send(player, type.netCodec().id(), buf);
    }

    @Override
    public <P> void onReceiveFromClient(Class<P> clazz, BiConsumer<P, ServerPlayerEntity> handler) {
        this.getClientPacketType(clazz).handlers().add(handler);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public <P> void sendToServer(P packet) {
        @SuppressWarnings("unchecked") ClientPacketType<P> type = (ClientPacketType<P>) getClientPacketType(packet.getClass());

        PacketByteBuf buf = PacketByteBufs.create();
        encodePacket(packet, buf, type.netCodec());
        ClientPlayNetworking.send(type.netCodec().id(), buf);
    }

    @Override
    @Environment(EnvType.CLIENT)
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
