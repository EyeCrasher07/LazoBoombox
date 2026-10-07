package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.LazoBoombox;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, LazoBoombox.MOD_ID);
    public static final Supplier<RecipeSerializer<BoomboxDyeRecipe>> DYE =
            SERIALIZERS.register(
                    "boombox_dye", () -> new CustomRecipe.Serializer<>(BoomboxDyeRecipe::new));

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
