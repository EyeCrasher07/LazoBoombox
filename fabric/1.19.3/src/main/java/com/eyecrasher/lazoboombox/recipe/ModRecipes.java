package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.LazoBoombox;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

public final class ModRecipes {
    public static final RecipeSerializer<BoomboxDyeRecipe> DYE =
            new SimpleCraftingRecipeSerializer<>(BoomboxDyeRecipe::new);

    private ModRecipes() {}

    public static void initialize() {
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                new ResourceLocation(LazoBoombox.MOD_ID, "boombox_dye"),
                DYE);
    }
}
