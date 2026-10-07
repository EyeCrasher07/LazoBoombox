package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.LazoBoombox;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;

public final class ModRecipes {
    public static final RecipeSerializer<BoomboxDyeRecipe> DYE =
            new SimpleRecipeSerializer<>(BoomboxDyeRecipe::new);

    private ModRecipes() {}

    public static void initialize() {
        Registry.register(
                Registry.RECIPE_SERIALIZER,
                new ResourceLocation(LazoBoombox.MOD_ID, "boombox_dye"),
                DYE);
    }
}
