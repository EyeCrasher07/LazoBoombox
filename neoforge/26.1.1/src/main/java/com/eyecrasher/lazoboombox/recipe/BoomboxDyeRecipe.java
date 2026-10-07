package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.data.BoomboxDyeColors;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Leather-style dyeing with one boombox and 1–8 dyes; previews never mutate inputs. */
public final class BoomboxDyeRecipe extends CustomRecipe {
    public BoomboxDyeRecipe() {}

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !findBoombox(input).isEmpty();
    }

    private static ItemStack findBoombox(CraftingInput input) {
        ItemStack boombox = ItemStack.EMPTY;
        int dyes = 0;
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == ModBlocks.BOOMBOX_ITEM.get()) {
                if (!boombox.isEmpty()) return ItemStack.EMPTY;
                boombox = stack;
            } else if (stack.has(net.minecraft.core.component.DataComponents.DYE)) {
                if (++dyes > 8) return ItemStack.EMPTY;
            } else return ItemStack.EMPTY;
        }
        return dyes > 0 ? boombox : ItemStack.EMPTY;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack boombox = findBoombox(input);
        if (boombox.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = boombox.copy();
        result.setCount(1);
        List<DyeColor> dyes = new ArrayList<>(8);
        for (int slot = 0; slot < input.size(); slot++)
            if (!input.getItem(slot).isEmpty()
                    && input.getItem(slot).getItem() != ModBlocks.BOOMBOX_ITEM.get())
                dyes.add(input.getItem(slot).get(net.minecraft.core.component.DataComponents.DYE));
        BoomboxData.writeColor(result, BoomboxDyeColors.mix(BoomboxData.readColor(result), dyes));
        return result;
    }

    @Override
    public RecipeSerializer<BoomboxDyeRecipe> getSerializer() {
        return ModRecipes.DYE.get();
    }
}
