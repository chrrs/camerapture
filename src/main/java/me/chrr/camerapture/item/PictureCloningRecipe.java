package me.chrr.camerapture.item;

import me.chrr.camerapture.Camerapture;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

public class PictureCloningRecipe extends SpecialCraftingRecipe {
    public PictureCloningRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        int paper = 0;
        ItemStack picture = ItemStack.EMPTY;

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(Camerapture.PICTURE)) {
                    if (!picture.isEmpty()) {
                        return false;
                    }

                    picture = stack;
                } else {
                    if (!stack.isOf(Items.PAPER)) {
                        return false;
                    }

                    ++paper;
                }
            }
        }

        return !picture.isEmpty() && picture.hasNbt() && paper > 0;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        int paper = 0;
        ItemStack picture = ItemStack.EMPTY;

        // We already know it matches, so we don't need as many checks.
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(Camerapture.PICTURE)) {
                    picture = stack;
                } else {
                    paper++;
                }
            }
        }

        return picture.copyWithCount(paper);
    }

    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
        DefaultedList<ItemStack> list = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);

        for (int i = 0; i < list.size(); ++i) {
            ItemStack stack = inventory.getStack(i);
            Item item = stack.getItem();
            if (stack.isOf(Camerapture.PICTURE)) {
                list.set(i, stack.copyWithCount(1));
            } else if (item.hasRecipeRemainder()) {
                //noinspection DataFlowIssue
                list.set(i, new ItemStack(item.getRecipeRemainder()));
            }
        }

        return list;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<PictureCloningRecipe> getSerializer() {
        return Camerapture.PICTURE_CLONING;
    }
}
