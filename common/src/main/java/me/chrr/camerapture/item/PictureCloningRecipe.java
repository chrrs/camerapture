package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class PictureCloningRecipe extends CustomRecipe {
    public static final PictureCloningRecipe INSTANCE = new PictureCloningRecipe();

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return getRecipe(input.items()).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return getRecipe(input.items()).map(Tuple::getA).orElse(null);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return getRecipe(input.items()).map(Tuple::getB).orElse(null);
    }

    private Optional<Tuple<ItemStack, NonNullList<ItemStack>>> getRecipe(List<ItemStack> items) {
        NonNullList<ItemStack> remainder = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        ItemStack picture = ItemStack.EMPTY;
        int paper = 0;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                if (stack.is(Camerapture.PICTURE)) {
                    if (!picture.isEmpty() || PictureItem.getPictureData(stack) == null) {
                        return Optional.empty();
                    }

                    remainder.set(i, stack.copyWithCount(1));
                    picture = stack;
                } else {
                    if (!stack.is(Items.PAPER)) {
                        return Optional.empty();
                    }

                    ++paper;
                }
            }
        }

        if (picture.isEmpty() || paper == 0) {
            return Optional.empty();
        } else {
            return Optional.of(new Tuple<>(picture.copyWithCount(paper), remainder));
        }
    }

    @Override
    public RecipeSerializer<PictureCloningRecipe> getSerializer() {
        return Camerapture.PICTURE_CLONING;
    }
}