package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.LazoBoombox;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class ModRecipes {
    // Both codecs must use the same stateless recipe instance.
    private static final BoomboxDyeRecipe DYE_RECIPE = new BoomboxDyeRecipe();
    public static final RecipeSerializer<BoomboxDyeRecipe> DYE =
            new RecipeSerializer<>(
                    com.mojang.serialization.MapCodec.unit(DYE_RECIPE),
                    net.minecraft.network.codec.StreamCodec.unit(DYE_RECIPE));

    private ModRecipes() {}

    public static void initialize() {
        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(LazoBoombox.MOD_ID, "boombox_dye"),
                DYE);
    }
}
