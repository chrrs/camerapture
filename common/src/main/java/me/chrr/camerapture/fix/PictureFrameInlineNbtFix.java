package me.chrr.camerapture.fix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;

public class PictureFrameInlineNbtFix extends DataFix {
    public PictureFrameInlineNbtFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        OpticFinder<?> opticFinder = DSL.namedChoice("camerapture:picture_frame", this.getInputSchema().getChoiceType(TypeReferences.ENTITY, "camerapture:picture_frame"));
        return this.fixTypeEverywhereTyped("InlineBlockPosFormatFix - Camerapture Picture Frame",
                this.getInputSchema().getType(TypeReferences.ENTITY), (entityTyped) ->
                        entityTyped.updateTyped(opticFinder, (pictureFrameTyped) ->
                                pictureFrameTyped.update(DSL.remainderFinder(), this::fixPictureFrameFields)));
    }

    private Dynamic<?> fixPictureFrameFields(Dynamic<?> dynamic) {
        dynamic = dynamic.renameField("Width", "width");
        dynamic = dynamic.renameField("Height", "height");

        dynamic = dynamic.renameField("Item", "item");
        dynamic = dynamic.renameField("PictureGlowing", "picture_glowing");
        dynamic = dynamic.renameField("Fixed", "fixed");
        dynamic = dynamic.renameField("Rotation", "rotation");

        dynamic = dynamic.renameAndFixField("Facing", "facing", (facing) ->
                switch (facing.asInt(0)) {
                    case 0 -> facing.createString("down");
                    case 1 -> facing.createString("up");
                    case 3 -> facing.createString("south");
                    case 4 -> facing.createString("west");
                    case 5 -> facing.createString("east");
                    default -> facing.createString("north");
                });

        return FixUtil.consolidateBlockPos(dynamic, "TileX", "TileY", "TileZ", "block_pos");
    }
}
