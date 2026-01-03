package me.chrr.camerapture.mixin;

import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import me.chrr.camerapture.fix.PictureFrameInlineNbtFix;
import net.minecraft.util.datafix.DataFixers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiFunction;

@Mixin(DataFixers.class)
public abstract class SchemasMixin {
    @Shadow
    @Final
    private static BiFunction<Integer, Schema, Schema> SAME_NAMESPACED;

    @Inject(method = "addFixers", at = @At(value = "TAIL"))
    private static void build(DataFixerBuilder builder, CallbackInfo ci) {
        Schema _1_21_6 = builder.addSchema(4430, SAME_NAMESPACED);
        builder.addFixer(new PictureFrameInlineNbtFix(_1_21_6));
    }
}
