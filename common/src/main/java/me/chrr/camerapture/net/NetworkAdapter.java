package me.chrr.camerapture.net;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;

/// An abstraction of network interfaces, since Fabric, Forge and NeoForge all use
/// different network abstractions. Note that all packets must be registered manually
/// if it's required, as Forge and NeoForge require this to happen at a specific moment.
public interface NetworkAdapter {
    <P> void sendToClient(ServerPlayer player, P packet);

    <P> void onReceiveFromClient(Class<P> clazz, BiConsumer<P, ServerPlayer> handler);

    <P> void sendToServer(P packet);

    <P> void onReceiveFromServer(Class<P> clazz, Consumer<P> handler);
}
