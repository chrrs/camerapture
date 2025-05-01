package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class AlbumCloningRecipe extends SpecialCraftingRecipe {
    public AlbumCloningRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        return getRecipe(inventory.getInputStacks()).isPresent();
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        return getRecipe(inventory.getInputStacks()).map(Pair::getLeft).orElse(null);
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
        return getRecipe(inventory.getInputStacks()).map(Pair::getRight).orElse(null);
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    private Optional<Pair<ItemStack, DefaultedList<ItemStack>>> getRecipe(List<ItemStack> items) {
        DefaultedList<ItemStack> remainder = DefaultedList.ofSize(items.size(), ItemStack.EMPTY);
        ItemStack album = ItemStack.EMPTY;
        boolean book = false;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(Camerapture.ALBUM)) {
                    if (!album.isEmpty()) {
                        return Optional.empty();
                    }

                    remainder.set(i, stack.copyWithCount(1));
                    album = stack;
                } else {
                    if (!stack.isOf(Items.WRITABLE_BOOK) || book) {
                        return Optional.empty();
                    }

                    book = true;
                }
            }
        }

        if (album.isEmpty() || !book) {
            return Optional.empty();
        } else {
            return Optional.of(new Pair<>(album.copy(), remainder));
        }
    }

    @Override
    public RecipeSerializer<AlbumCloningRecipe> getSerializer() {
        return Camerapture.ALBUM_CLONING;
    }
}