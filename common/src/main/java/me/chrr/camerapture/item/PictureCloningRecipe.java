package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class PictureCloningRecipe extends SpecialCraftingRecipe {
    public PictureCloningRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        return getRecipe(input.getStacks()).isPresent();
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        return getRecipe(input.getStacks()).map(Pair::getLeft).orElse(null);
    }

    @Override
    public DefaultedList<ItemStack> getRecipeRemainders(CraftingRecipeInput input) {
        return getRecipe(input.getStacks()).map(Pair::getRight).orElse(null);
    }

    private Optional<Pair<ItemStack, DefaultedList<ItemStack>>> getRecipe(List<ItemStack> items) {
        DefaultedList<ItemStack> remainder = DefaultedList.ofSize(items.size(), ItemStack.EMPTY);
        ItemStack picture = ItemStack.EMPTY;
        int paper = 0;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(Camerapture.PICTURE)) {
                    if (!picture.isEmpty() || PictureItem.getPictureData(stack) == null) {
                        return Optional.empty();
                    }

                    remainder.set(i, stack.copyWithCount(1));
                    picture = stack;
                } else {
                    if (!stack.isOf(Items.PAPER)) {
                        return Optional.empty();
                    }

                    ++paper;
                }
            }
        }

        if (picture.isEmpty() || paper == 0) {
            return Optional.empty();
        } else {
            return Optional.of(new Pair<>(picture.copyWithCount(paper), remainder));
        }
    }

    @Override
    public RecipeSerializer<PictureCloningRecipe> getSerializer() {
        return Camerapture.PICTURE_CLONING;
    }
}