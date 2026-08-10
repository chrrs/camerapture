package me.chrr.camerapture.render;

import me.chrr.camerapture.block.PictureFrameBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientPictureFrameTracker {
    public static final Set<PictureFrameBlockEntity> TRACKED_FRAMES = ConcurrentHashMap.newKeySet();

    public static void add(PictureFrameBlockEntity blockEntity) {
        if (blockEntity != null) {
            TRACKED_FRAMES.add(blockEntity);
        }
    }

    public static void remove(PictureFrameBlockEntity blockEntity) {
        if (blockEntity != null) {
            TRACKED_FRAMES.remove(blockEntity);
        }
    }

    public static void clear() {
        TRACKED_FRAMES.clear();
    }

    public static void ensureFramesExtracted(LevelRenderState levelRenderState, float partialTick) {
        if (TRACKED_FRAMES.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }

        BlockEntityRenderDispatcher dispatcher = client.getBlockEntityRenderDispatcher();

        // Clean up invalid/removed block entities
        TRACKED_FRAMES.removeIf(be -> be == null || be.isRemoved() || !be.hasLevel() || be.getLevel() != client.level);

        for (PictureFrameBlockEntity blockEntity : TRACKED_FRAMES) {
            boolean alreadyExtracted = false;
            for (BlockEntityRenderState state : levelRenderState.blockEntityRenderStates) {
                if (blockEntity.getBlockPos().equals(state.blockPos)) {
                    alreadyExtracted = true;
                    break;
                }
            }

            if (!alreadyExtracted) {
                BlockEntityRenderState extractedState = dispatcher.tryExtractRenderState(blockEntity, partialTick, null, true);
                if (extractedState != null) {
                    levelRenderState.blockEntityRenderStates.add(extractedState);
                }
            }
        }
    }
}
