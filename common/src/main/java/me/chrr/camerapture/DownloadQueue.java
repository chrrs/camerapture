package me.chrr.camerapture;

import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.picture.StoredPicture;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// The download queue maintains a list of pictures to be sent to players. This
/// exists to limit network activity if many players are trying to download pictures
/// at the same time, for example when someone logs in.
///
/// Pictures are held in a separate queue per player and drained round-robin, one picture per
/// interval. A single global queue would let one player asking for a large area monopolise the
/// server's entire picture bandwidth — at the default 20ms interval that's 50 pictures a second
/// shared by everyone, so a player loading a few thousand posters would stall every other player
/// for a minute or more. Round-robin bounds a player's impact to their own share.
public class DownloadQueue {
    private static final DownloadQueue INSTANCE = new DownloadQueue();

    private final Object lock = new Object();

    /// Per-player queues, plus the rotation order they're drained in.
    private final Map<UUID, PlayerQueue> byPlayer = new HashMap<>();
    private final Deque<PlayerQueue> rotation = new ArrayDeque<>();

    private ScheduledExecutorService scheduler;

    private DownloadQueue() {
    }

    /// Schedule a picture to be sent. Requesting a picture that's already queued for the same player
    /// is a no-op, so a client that asks repeatedly while its first request is still in flight
    /// doesn't multiply the work.
    public void send(ServerPlayer player, UUID id, StoredPicture picture) {
        synchronized (lock) {
            PlayerQueue queue = byPlayer.get(player.getUUID());
            if (queue == null) {
                queue = new PlayerQueue(player);
                byPlayer.put(player.getUUID(), queue);
                rotation.add(queue);
            }

            if (!queue.pendingIds.add(id)) {
                return;
            }

            queue.pending.add(new QueuedPicture(id, picture));
        }
    }

    /// Start processing the queue and send a picture every so often.
    public void start(long intervalMs) {
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::processQueue, 0, intervalMs, TimeUnit.MILLISECONDS);
        }
    }

    /// Stop processing the queue and clear it.
    public void stop() {
        if (scheduler != null) {
            synchronized (lock) {
                byPlayer.clear();
                rotation.clear();
            }

            scheduler.shutdown();
            scheduler = null;
        }
    }

    /// Send a single picture, taking the next player in the rotation.
    private void processQueue() {
        ServerPlayer recipient;
        QueuedPicture item;

        synchronized (lock) {
            while (true) {
                PlayerQueue queue = rotation.poll();
                if (queue == null) {
                    return;
                }

                if (queue.player.hasDisconnected()) {
                    byPlayer.remove(queue.player.getUUID());
                    continue;
                }

                item = queue.pending.poll();
                if (item == null) {
                    byPlayer.remove(queue.player.getUUID());
                    continue;
                }

                queue.pendingIds.remove(item.id);
                recipient = queue.player;

                // Back of the rotation, so every other waiting player gets a turn first.
                if (queue.pending.isEmpty()) {
                    byPlayer.remove(queue.player.getUUID());
                } else {
                    rotation.add(queue);
                }

                break;
            }
        }

        // Splitting copies the whole picture, so keep it off the lock.
        final ServerPlayer target = recipient;
        final QueuedPicture sending = item;
        ByteCollector.split(sending.picture().bytes(), Camerapture.SERVER_SECTION_SIZE, (section, bytesLeft) ->
                Camerapture.NETWORK.sendToClient(target, new DownloadPartialPicturePacket(sending.id(), section, bytesLeft)));
    }

    public static DownloadQueue getInstance() {
        return INSTANCE;
    }

    private static class PlayerQueue {
        private final ServerPlayer player;
        private final Deque<QueuedPicture> pending = new ArrayDeque<>();
        private final Set<UUID> pendingIds = new HashSet<>();

        private PlayerQueue(ServerPlayer player) {
            this.player = player;
        }
    }

    private record QueuedPicture(UUID id, StoredPicture picture) {
    }
}
