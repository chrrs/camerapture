package me.chrr.camerapture.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(Schema1460.class)
public abstract class Schema1460Mixin {
    @Inject(method = "registerEntities", at = @At(value = "RETURN"))
    private void registerEntities(Schema schema, CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> cir) {
        Map<String, Supplier<TypeTemplate>> map = cir.getReturnValue();

        schema.register(
                map, "camerapture:picture_frame",
                name -> DSL.optionalFields("Item", TypeReferences.ITEM_STACK.in(schema))
        );
    }
}