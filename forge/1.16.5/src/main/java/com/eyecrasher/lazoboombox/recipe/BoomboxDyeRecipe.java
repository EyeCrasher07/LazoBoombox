package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.data.BoomboxDyeColors;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.DyeColor;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/** Leather-style dyeing with one boombox and 1–8 dyes; previews never mutate inputs. */
public final class BoomboxDyeRecipe extends SpecialRecipe {
    public BoomboxDyeRecipe(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean matches(CraftingInventory input, World level) {
        return !findBoombox(input).isEmpty();
    }

    private static ItemStack findBoombox(CraftingInventory input) {
        ItemStack boombox = ItemStack.EMPTY;
        int dyes = 0;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == ModBlocks.BOOMBOX_ITEM.get()) {
                if (!boombox.isEmpty()) return ItemStack.EMPTY;
                boombox = stack;
            } else if (stack.getItem() instanceof DyeItem) {
                if (++dyes > 8) return ItemStack.EMPTY;
            } else return ItemStack.EMPTY;
        }
        return dyes > 0 ? boombox : ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingInventory input) {
        ItemStack boombox = findBoombox(input);
        if (boombox.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = boombox.copy();
        result.setCount(1);
        List<DyeColor> dyes = new ArrayList<>(8);
        for (int slot = 0; slot < input.getContainerSize(); slot++)
            if (!input.getItem(slot).isEmpty()
                    && input.getItem(slot).getItem() != ModBlocks.BOOMBOX_ITEM.get())
                dyes.add(((DyeItem) input.getItem(slot).getItem()).getDyeColor());
        BoomboxData.writeColor(result, BoomboxDyeColors.mix(BoomboxData.readColor(result), dyes));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public IRecipeSerializer<BoomboxDyeRecipe> getSerializer() {
        return ModRecipes.DYE.get();
    }
}
