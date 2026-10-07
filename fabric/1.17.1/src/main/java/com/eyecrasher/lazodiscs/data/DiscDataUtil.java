package com.eyecrasher.lazodiscs.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

import java.util.Optional;
import java.util.UUID;

public final class DiscDataUtil {
    public static final String ROOT_KEY = "lazodiscs";
    private static final String URL_KEY = "url",
            TITLE_KEY = "title",
            RANGE_KEY = "range",
            VOLUME_KEY = "volume",
            ID_KEY = "id";

    private DiscDataUtil() {}

    public static boolean isMusicDisc(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof RecordItem;
    }

    public static boolean hasCustomDisc(ItemStack stack) {
        return read(stack).isPresent();
    }

    public static Optional<CustomDiscData> read(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        CompoundTag root = stack.getTag();
        if (root == null) return Optional.empty();
        if (!root.contains(ROOT_KEY)) return Optional.empty();
        CompoundTag tag = root.getCompound(ROOT_KEY);
        if (!tag.contains(URL_KEY) || !tag.contains(ID_KEY)) return Optional.empty();
        try {
            String url = tag.getString(URL_KEY), title = tag.getString(TITLE_KEY);
            int range = tag.getInt(RANGE_KEY);
            float volume = tag.getFloat(VOLUME_KEY);
            UUID id = UUID.fromString(tag.getString(ID_KEY));
            return Optional.of(new CustomDiscData(url, title, range, volume, id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void write(ItemStack stack, CustomDiscData disc) {
        CompoundTag root = stack.getTag() == null ? new CompoundTag() : stack.getTag().copy();
        CompoundTag tag = new CompoundTag();
        tag.putString(URL_KEY, disc.url());
        tag.putString(TITLE_KEY, disc.title());
        tag.putInt(RANGE_KEY, disc.range());
        tag.putFloat(VOLUME_KEY, disc.volume());
        tag.putString(ID_KEY, disc.id().toString());
        root.put(ROOT_KEY, tag);
        stack.setTag(root);
    }

    public static void clear(ItemStack stack) {
        CompoundTag old = stack.getTag();
        if (old == null) return;
        CompoundTag root = old.copy();
        root.remove(ROOT_KEY);
        stack.setTag(root.isEmpty() ? null : root);
    }
}
