package me.chrr.camerapture.item;

//? if >=1.21 {
import net.minecraft.item.tooltip.TooltipData;
 //?} else
/*import net.minecraft.client.item.TooltipData;*/

import java.util.UUID;

public record PictureTooltipData(UUID uuid) implements TooltipData {
}