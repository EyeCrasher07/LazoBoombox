package com.eyecrasher.lazoboombox.data;

import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.minecraft.item.ItemStack;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ITextComponent;

public final class DiscNameUtil {
    private DiscNameUtil() {}

    public static ITextComponent get(ItemStack stack, DynamicRegistries registries) {
        if (stack == null || stack.isEmpty())
            return new net.minecraft.util.text.StringTextComponent("?");
        CustomDiscData data = DiscDataUtil.read(stack).orElse(null);
        if (data != null) return new net.minecraft.util.text.StringTextComponent(data.title());
        if (stack.getItem() instanceof MusicDiscItem record) return record.getDisplayName();
        return stack.getHoverName();
    }
}
