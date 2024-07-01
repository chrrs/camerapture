package me.chrr.camerapture.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

//? if >=1.20.5 {
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
//?} else {
/*import me.chrr.camerapture.Camerapture;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.PacketByteBuf;
*///?}

//? if <1.20.3
/*import net.minecraft.nbt.NbtCompound;*/

public class Networking {
    private static final Map<Class<?>, ServerPacketType<?>> serverPackets = new HashMap<>();
    private static final Map<Class<?>, ClientPacketType<?>> clientPackets = new HashMap<>();

    private static ClientAdapter clientAdapter = null;

    private Networking() {
    }

    public static <P> void registerServerBound(Class<P> clazz, NetCodec<P> netCodec) {
        ClientPacketType<P> type = new ClientPacketType<>(netCodec, new ArrayList<>());
        clientPackets.put(clazz, type);

        //? if >=1.20.5 {
        PacketCodec<io.netty.buffer.ByteBuf, P> codec = PacketCodecs.codec(netCodec.codec());
        PayloadTypeRegistry.playC2S().register(
                new CustomPayload.Id<PacketPayload<P>>(netCodec.id()),
                PacketCodec.tuple(codec, PacketPayload::packet, p -> new PacketPayload<>(netCodec.id(), p))
        );

        ServerPlayNetworking.registerGlobalReceiver(
                new CustomPayload.Id<PacketPayload<P>>(netCodec.id()),
                (payload, context) -> type.handlers().forEach(handler -> handler.accept(payload.packet, context.player()))
        );
        //?} else {
        /*ServerPlayNetworking.registerGlobalReceiver(netCodec.id(),
                (server, player, networkHandler, buf, sender) -> {
                    MapCodec<P> dataCodec = type.netCodec().codec().fieldOf("data");
                    DataResult<Pair<P, NbtElement>> result = dataCodec.decoder().decode(NbtOps.INSTANCE, buf.readNbt());

                    if (result.error().isPresent()) {
                        Camerapture.LOGGER.error("failed to decode packet: {}", result.error().get().message());
                        return;
                    }

                    P packet = result.result().orElseThrow().getFirst();
                    type.handlers().forEach(handler -> handler.accept(packet, player));
                }
        );
        *///?}
    }

    public static <P> void registerClientBound(Class<P> clazz, NetCodec<P> netCodec) {
        ServerPacketType<P> type = new ServerPacketType<>(netCodec, new ArrayList<>());
        serverPackets.put(clazz, type);

        //? if >=1.20.5 {
        PacketCodec<io.netty.buffer.ByteBuf, P> codec = PacketCodecs.codec(netCodec.codec());
        PayloadTypeRegistry.playS2C().register(
                new CustomPayload.Id<PacketPayload<P>>(netCodec.id()),
                PacketCodec.tuple(codec, PacketPayload::packet, p -> new PacketPayload<>(netCodec.id(), p))
        );
        //?}

        if (clientAdapter != null) {
            clientAdapter.registerHandler(type);
        }
    }

    public static <P> void sendTo(ServerPlayerEntity player, P packet) {
        @SuppressWarnings("unchecked") ServerPacketType<P> type = (ServerPacketType<P>) getServerPacketType(packet.getClass());

        //? if >=1.20.5 {
        PacketPayload<P> payload = new PacketPayload<>(type.netCodec().id(), packet);
        ServerPlayNetworking.send(player, payload);
        //?} else {
        /*PacketByteBuf buf = PacketByteBufs.create();

        MapCodec<P> dataCodec = type.netCodec().codec().fieldOf("data");
        DataResult<NbtElement> result = dataCodec.encoder().encodeStart(NbtOps.INSTANCE, packet);

        //? if >=1.20.3 {
        buf.writeNbt(result.get().left().orElseThrow());
        //?} else
        /^buf.writeNbt((NbtCompound) result.get().left().orElseThrow());^/

        ServerPlayNetworking.send(player, type.netCodec().id(), buf);
        *///?}
    }

    public static <P> void onServerPacketReceive(Class<P> clazz, BiConsumer<P, ServerPlayerEntity> handler) {
        getClientPacketType(clazz).handlers().add(handler);
    }

    public static <P> ServerPacketType<P> getServerPacketType(Class<P> clazz) {
        @SuppressWarnings("unchecked") ServerPacketType<P> type = (ServerPacketType<P>) serverPackets.get(clazz);
        return type;
    }

    public static <P> ClientPacketType<P> getClientPacketType(Class<P> clazz) {
        @SuppressWarnings("unchecked") ClientPacketType<P> type = (ClientPacketType<P>) clientPackets.get(clazz);
        return type;
    }

    public static void setClientAdapter(ClientAdapter adapter) {
        Networking.clientAdapter = adapter;
        serverPackets.values().forEach(adapter::registerHandler);
    }

    //? if >=1.20.5 {
    public record PacketPayload<P>(Identifier id, P packet) implements CustomPayload {
        @Override
        public Id<PacketPayload<P>> getId() {
            return new Id<>(id);
        }
    }
    //?}

    public record ClientPacketType<P>(NetCodec<P> netCodec, List<BiConsumer<P, ServerPlayerEntity>> handlers) {
    }

    public record ServerPacketType<P>(NetCodec<P> netCodec, List<Consumer<P>> handlers) {
    }
}
