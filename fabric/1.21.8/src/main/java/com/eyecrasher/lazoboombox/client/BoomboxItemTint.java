package com.eyecrasher.lazoboombox.client;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class BoomboxItemTint implements ItemTintSource {
    public static final MapCodec<BoomboxItemTint> MAP_CODEC = MapCodec.unit(BoomboxItemTint::new);

    @Override
    public int calculate(ItemStack stack, ClientLevel level, LivingEntity entity) {
        return BoomboxColors.itemColor(stack, 0);
    }

    @Override
    public MapCodec<BoomboxItemTint> type() {
        return MAP_CODEC;
    }
}
