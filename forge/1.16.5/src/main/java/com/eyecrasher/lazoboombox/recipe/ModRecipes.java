package com.eyecrasher.lazoboombox.recipe;

import com.eyecrasher.lazoboombox.LazoBoombox;

import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.SpecialRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class ModRecipes {
    private static final DeferredRegister<IRecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, LazoBoombox.MOD_ID);
    public static final Supplier<IRecipeSerializer<BoomboxDyeRecipe>> DYE =
            SERIALIZERS.register(
                    "boombox_dye", () -> new SpecialRecipeSerializer<>(BoomboxDyeRecipe::new));

    private ModRecipes() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
