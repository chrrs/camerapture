package me.chrr.camerapture.compat.distantdecorations;

import me.chrr.camerapture.Camerapture;
import me.chrr.camerapture.block.PictureFrameBlockEntity;
import me.chrr.camerapture.item.PictureItem;
import me.justbecause.distantdecorations.api.DecorationProvider;
import me.justbecause.distantdecorations.api.DecorationRegistry;
import me.justbecause.distantdecorations.api.DecorationType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CameraptureDistantDecorationProvider implements DecorationProvider<CameraptureDistantData> {

    public static final Identifier TYPE_ID = Identifier.fromNamespaceAndPath(Camerapture.MOD_ID, "picture_frame");

    public static final DecorationType<CameraptureDistantData> TYPE = new DecorationType<>(
        TYPE_ID,
        (data, buf) -> {
            UUIDUtil.STREAM_CODEC.encode(buf, data.pictureId());
            buf.writeByte(data.facing().get2DDataValue());
            buf.writeVarInt(data.width());
            buf.writeVarInt(data.height());
            buf.writeVarInt(data.rotation());
            buf.writeBoolean(data.isGlowing());
        },
        buf -> new CameraptureDistantData(
            UUIDUtil.STREAM_CODEC.decode(buf),
            Direction.from2DDataValue(buf.readByte()),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readBoolean()
        )
    );

    public static void init() {
        DecorationRegistry.registerProvider(new CameraptureDistantDecorationProvider());
    }

    @Override
    public DecorationType<CameraptureDistantData> type() {
        return TYPE;
    }

    @Override
    public boolean matches(BlockEntity blockEntity) {
        return blockEntity instanceof PictureFrameBlockEntity;
    }

    @Override
    public @Nullable CameraptureDistantData capture(ServerLevel level, BlockPos pos, BlockEntity blockEntity) {
        if (blockEntity instanceof PictureFrameBlockEntity pfbe) {
            ItemStack stack = pfbe.getItemStack();
            if (stack == null || stack.isEmpty()) {
                return null;
            }
            PictureItem.PictureData pictureData = PictureItem.getPictureData(stack);
            if (pictureData == null) {
                return null;
            }
            return new CameraptureDistantData(
                pictureData.id(),
                pfbe.getFacing(),
                pfbe.getFrameWidth(),
                pfbe.getFrameHeight(),
                pfbe.getRotation(),
                pfbe.isPictureGlowing()
            );
        }
        return null;
    }

    @Override
    public AABB calculateBounds(ServerLevel level, BlockPos pos, CameraptureDistantData data) {
        // Compute bounding box matching PictureFrameBlockEntity.getRenderBox()
        double thickness = 0.0625;
        double minX, maxX, minZ, maxZ;
        switch (data.facing()) {
            case SOUTH -> {
                minX = 0.0;
                maxX = data.width();
                minZ = 0.0;
                maxZ = thickness;
            }
            case EAST -> {
                minX = 0.0;
                maxX = thickness;
                minZ = 1.0 - data.width();
                maxZ = 1.0;
            }
            case WEST -> {
                minX = 1.0 - thickness;
                maxX = 1.0;
                minZ = 0.0;
                maxZ = data.width();
            }
            default -> { // NORTH
                minX = 1.0 - data.width();
                maxX = 1.0;
                minZ = 1.0 - thickness;
                maxZ = 1.0;
            }
        }
        return new AABB(
            pos.getX() + minX, pos.getY(), pos.getZ() + minZ,
            pos.getX() + maxX, pos.getY() + data.height(), pos.getZ() + maxZ
        ).inflate(0.5);
    }
}
