package me.chrr.camerapture;

import me.chrr.camerapture.net.clientbound.DownloadPartialPicturePacket;
import me.chrr.camerapture.picture.StoredPicture;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// The download queue maintains a list of pictures to be sent to players. This
/// exists to limit network activity if many players are trying to download pictures
/// at the same time, for example when someone logs in.
public class DownloadQueue {
    private static final DownloadQueue INSTANCE = new DownloadQueue();

    private final Queue<QueuedPicture> queue = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService scheduler;

    private DownloadQueue() {
    }

    /// Schedule a picture to be sent.
    public void send(ServerPlayerEntity player, UUID id, StoredPicture picture) {
        queue.add(new QueuedPicture(player, id, picture));
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
            queue.clear();
            scheduler.shutdown();
            scheduler = null;
        }
    }

    /// Process a single item in the queue.
    private void processQueue() {
        QueuedPicture item = queue.poll();
        if (item == null || item.recipient.isDisconnected()) {
            return;
        }

        ByteCollector.split(item.picture.bytes(), Camerapture.SECTION_SIZE, (section, bytesLeft) ->
                Camerapture.NETWORK.sendToClient(item.recipient, new DownloadPartialPicturePacket(item.id, section, bytesLeft)));
    }

    public static DownloadQueue getInstance() {
        return INSTANCE;
    }

    private record QueuedPicture(ServerPlayerEntity recipient, UUID id, StoredPicture picture) {
    }
}
