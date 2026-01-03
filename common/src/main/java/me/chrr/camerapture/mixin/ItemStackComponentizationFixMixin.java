package me.chrr.camerapture.mixin;

import com.mojang.serialization.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;

@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
    @Inject(method = "fixItemStack", at = @At(value = "TAIL"))
    private static void fixStack(ItemStackComponentizationFix.ItemStackData data, Dynamic<?> dynamic, CallbackInfo ci) {
        if (data.is("camerapture:picture")) {
            camerapture$fixPicture(data, dynamic);
        }

        if (data.is("camerapture:album")) {
            camerapture$fixAlbum(data, dynamic);
        }

        // For the camera, we just remove the 'active' tag.
        // It should be inactive by default.
        if (data.is("camerapture:camera")) {
            data.removeTag("active");
        }
    }

    @Unique
    private static void camerapture$fixPicture(ItemStackComponentizationFix.ItemStackData data, Dynamic<?> dynamic) {
        Optional<? extends Dynamic<?>> uuid = data.removeTag("uuid").result();
        if (uuid.isPresent()) {
            String creator = data.removeTag("creator").asString("");
            long timestamp = data.removeTag("timestamp").asLong(0L);

            data.setComponent("camerapture:picture_data", dynamic.emptyMap()
                    .set("id", uuid.get())
                    .set("creator", dynamic.createString(creator))
                    .set("timestamp", dynamic.createLong(timestamp)));
        }
    }

    @Unique
    private static void camerapture$fixAlbum(ItemStackComponentizationFix.ItemStackData data, Dynamic<?> dynamic) {
        List<? extends Dynamic<?>> list = data.removeTag("Items")
                .asList(itemsDynamic -> itemsDynamic.emptyMap()
                        .set("slot", itemsDynamic.createInt(itemsDynamic.get("Slot").asByte((byte) 0)))
                        .set("item", itemsDynamic.remove("Slot"))
                );

        if (!list.isEmpty()) {
            data.setComponent("minecraft:container", dynamic.createList(list.stream()));
        }
    }
}