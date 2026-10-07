package com.eyecrasher.lazoboombox.data;

import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

public final class DiscNameUtil {
    private DiscNameUtil() {}

    public static Component get(ItemStack stack, RegistryAccess registries) {
        if (stack == null || stack.isEmpty())
            return net.minecraft.network.chat.Component.literal("?");
        CustomDiscData data = DiscDataUtil.read(stack).orElse(null);
        if (data != null) return net.minecraft.network.chat.Component.literal(data.title());
        if (stack.getItem() instanceof RecordItem record) return record.getDisplayName();
        return stack.getHoverName();
    }
}
