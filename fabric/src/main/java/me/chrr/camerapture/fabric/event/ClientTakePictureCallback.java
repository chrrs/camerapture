package me.chrr.camerapture.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.util.ActionResult;

public interface ClientTakePictureCallback {
    Event<ClientTakePictureCallback> EVENT = EventFactory.createArrayBacked(ClientTakePictureCallback.class,
            (listeners) -> () -> {
                for (ClientTakePictureCallback listener : listeners) {
                    ActionResult result = listener.takePicture();
                    if (result != ActionResult.PASS)
                        return result;
                }

                return ActionResult.PASS;
            });

    ActionResult takePicture();
}
