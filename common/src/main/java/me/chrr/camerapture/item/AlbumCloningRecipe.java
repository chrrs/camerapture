package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import java.util.List;
import java.util.Optional;

public class AlbumCloningRecipe extends CustomRecipe {
    public AlbumCloningRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return getRecipe(input.items()).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return getRecipe(input.items()).map(Tuple::getA).orElse(null);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        return getRecipe(input.items()).map(Tuple::getB).orElse(null);
    }

    private Optional<Tuple<ItemStack, NonNullList<ItemStack>>> getRecipe(List<ItemStack> items) {
        NonNullList<ItemStack> remainder = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        ItemStack album = ItemStack.EMPTY;
        boolean book = false;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                if (stack.is(Camerapture.ALBUM)) {
                    if (!album.isEmpty()) {
                        return Optional.empty();
                    }

                    remainder.set(i, stack.copyWithCount(1));
                    album = stack;
                } else {
                    if (!stack.is(Items.WRITABLE_BOOK) || book) {
                        return Optional.empty();
                    }

                    book = true;
                }
            }
        }

        if (album.isEmpty() || !book) {
            return Optional.empty();
        } else {
            return Optional.of(new Tuple<>(album.copy(), remainder));
        }
    }

    @Override
    public RecipeSerializer<AlbumCloningRecipe> getSerializer() {
        return Camerapture.ALBUM_CLONING;
    }
}