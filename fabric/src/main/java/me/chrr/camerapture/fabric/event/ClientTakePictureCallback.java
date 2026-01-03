package me.chrr.camerapture.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.InteractionResult;

public interface ClientTakePictureCallback {
    Event<ClientTakePictureCallback> EVENT = EventFactory.createArrayBacked(ClientTakePictureCallback.class,
            (listeners) -> () -> {
                for (ClientTakePictureCallback listener : listeners) {
                    InteractionResult result = listener.takePicture();
                    if (result != InteractionResult.PASS)
                        return result;
                }

                return InteractionResult.PASS;
            });

    InteractionResult takePicture();
}
