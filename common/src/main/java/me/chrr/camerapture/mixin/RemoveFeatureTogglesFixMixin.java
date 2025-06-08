package me.chrr.camerapture.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.FixUtil;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.fix.RemoveFeatureTogglesFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

// Putting 1.21.6 inline-block-pos-format for picture frames in here.
@Mixin(RemoveFeatureTogglesFix.class)
public abstract class RemoveFeatureTogglesFixMixin extends DataFix {
    // Dummy constructor to match super.
    private RemoveFeatureTogglesFixMixin(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @WrapOperation(method = "makeRule", at = @At(value = "INVOKE", target = "Lnet/minecraft/datafixer/fix/RemoveFeatureTogglesFix;fixTypeEverywhereTyped(Ljava/lang/String;Lcom/mojang/datafixers/types/Type;Ljava/util/function/Function;)Lcom/mojang/datafixers/TypeRewriteRule;"))
    public TypeRewriteRule makeRule(RemoveFeatureTogglesFix instance, String s, Type<?> type, Function<?, ?> function, Operation<TypeRewriteRule> original) {
        OpticFinder<?> opticFinder = DSL.namedChoice("camerapture:picture_frame", this.getInputSchema().getChoiceType(TypeReferences.ENTITY, "camerapture:picture_frame"));
        TypeRewriteRule rule = this.fixTypeEverywhereTyped("InlineBlockPosFormatFix - Camerapture Picture Frame",
                this.getInputSchema().getType(TypeReferences.ENTITY), (entityTyped) ->
                        entityTyped.updateTyped(opticFinder, (pictureFrameTyped) ->
                                pictureFrameTyped.update(DSL.remainderFinder(), this::camerapture$fixPictureFrameFields)));

        return TypeRewriteRule.seq(original.call(instance, s, type, function), rule);
    }

    @Unique
    private Dynamic<?> camerapture$fixPictureFrameFields(Dynamic<?> dynamic) {
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
