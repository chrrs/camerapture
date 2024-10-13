package me.chrr.camerapture.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

//? if >=1.20.5 {
import net.minecraft.network.packet.CustomPayload;
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

import java.util.function.Consumer;

public class ClientNetworking implements ClientAdapter {
    private ClientNetworking() {
    }

    public static void init() {
        Networking.setClientAdapter(new ClientNetworking());
    }

    public static <P> void sendToServer(P packet) {
        @SuppressWarnings("unchecked")
        Networking.ClientPacketType<P> type = (Networking.ClientPacketType<P>) Networking.getClientPacketType(packet.getClass());

        //? if >=1.20.5 {
        Networking.PacketPayload<?> payload = new Networking.PacketPayload<>(type.netCodec().id(), packet);
        ClientPlayNetworking.send(payload);
        //?} else {
        /*PacketByteBuf buf = PacketByteBufs.create();

        MapCodec<P> dataCodec = type.netCodec().codec().fieldOf("data");
        DataResult<NbtElement> result = dataCodec.encoder().encodeStart(NbtOps.INSTANCE, packet);

        //? if >=1.20.3 {
        /^buf.writeNbt(result.get().left().orElseThrow());
         ^///?} else
        buf.writeNbt((NbtCompound) result.get().left().orElseThrow());

        ClientPlayNetworking.send(type.netCodec().id(), buf);
        *///?}
    }

    public static <P> void onClientPacketReceive(Class<P> clazz, Consumer<P> handler) {
        Networking.getServerPacketType(clazz).handlers().add(handler);
    }

    @Override
    public <P> void registerHandler(Networking.ServerPacketType<P> type) {
        //? if >=1.20.5 {
        ClientPlayNetworking.registerGlobalReceiver(
                new CustomPayload.Id<Networking.PacketPayload<P>>(type.netCodec().id()),
                (payload, context) -> type.handlers().forEach(handler -> handler.accept(payload.packet()))
        );
        //?} else {
        /*ClientPlayNetworking.registerGlobalReceiver(type.netCodec().id(),
                (client, networkHandler, buf, sender) -> {
                    MapCodec<P> dataCodec = type.netCodec().codec().fieldOf("data");
                    DataResult<Pair<P, NbtElement>> result = dataCodec.decoder().decode(NbtOps.INSTANCE, buf.readNbt());

                    if (result.error().isPresent()) {
                        Camerapture.LOGGER.error("failed to decode packet: {}", result.error().get().message());
                        return;
                    }

                    P packet = result.result().orElseThrow().getFirst();
                    type.handlers().forEach(handler -> handler.accept(packet));
                }
        );
        *///?}
    }
}
