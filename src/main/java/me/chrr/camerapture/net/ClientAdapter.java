package me.chrr.camerapture.net;

public interface ClientAdapter {
    <P> void registerHandler(Networking.ServerPacketType<P> type);
}
