package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.LazoBoombox;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, LazoBoombox.MOD_ID);
    public static final Supplier<RecipeSerializer<BoomboxDyeRecipe>> DYE =
            SERIALIZERS.register(
                    "boombox_dye", () -> new SimpleRecipeSerializer<>(BoomboxDyeRecipe::new));

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
