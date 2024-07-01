package me.chrr.camerapture.mixin;

//? if >=1.20.5 {
import com.mojang.serialization.Dynamic;
import net.minecraft.datafixer.fix.ItemStackComponentizationFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(ItemStackComponentizationFix.class)
public class ItemStackComponentizationFixMixin {
    @Inject(method = "fixStack", at = @At(value = "TAIL"))
    private static void fixStack(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic, CallbackInfo ci) {
        if (data.itemEquals("camerapture:picture")) {
            fixPicture(data, dynamic);
        }

        if (data.itemEquals("camerapture:album")) {
            fixAlbum(data, dynamic);
        }

        // For the camera, we just remove the 'active' tag.
        // It should be inactive by default.
        if (data.itemEquals("camerapture:camera")) {
            data.getAndRemove("active");
        }
    }

    @Unique
    private static void fixPicture(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
        Optional<? extends Dynamic<?>> uuid = data.getAndRemove("uuid").result();
        if (uuid.isPresent()) {
            String creator = data.getAndRemove("creator").asString("");
            long timestamp = data.getAndRemove("timestamp").asLong(0L);

            data.setComponent("camerapture:picture_data", dynamic.emptyMap()
                    .set("id", uuid.get())
                    .set("creator", dynamic.createString(creator))
                    .set("timestamp", dynamic.createLong(timestamp)));
        }
    }

    @Unique
    private static void fixAlbum(ItemStackComponentizationFix.StackData data, Dynamic<?> dynamic) {
        List<? extends Dynamic<?>> list = data.getAndRemove("Items")
                .asList(itemsDynamic -> itemsDynamic.emptyMap()
                        .set("slot", itemsDynamic.createInt(itemsDynamic.get("Slot").asByte((byte) 0)))
                        .set("item", itemsDynamic.remove("Slot"))
                );

        if (!list.isEmpty()) {
            data.setComponent("minecraft:container", dynamic.createList(list.stream()));
        }
    }
}
//?} else {
/*// FIXME: Dummy class to keep Mixin happy. Is there a better way to do this?
//        Changing camerapture.mixins.json with Stonecutter is not that easy,
//        so this is our next best option.

import me.chrr.camerapture.Camerapture;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Camerapture.class)
public class ItemStackComponentizationFixMixin {
}
*///?}