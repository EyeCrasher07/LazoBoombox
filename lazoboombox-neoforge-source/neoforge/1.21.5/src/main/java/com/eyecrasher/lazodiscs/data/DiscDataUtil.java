package com.eyecrasher.lazodiscs.data;
import net.minecraft.core.component.DataComponents; import net.minecraft.nbt.CompoundTag; import net.minecraft.world.item.ItemStack; import net.minecraft.world.item.component.CustomData;
import java.util.Optional; import java.util.UUID;
public final class DiscDataUtil {
    public static final String ROOT_KEY = "lazodiscs";
    private static final String URL_KEY = "url", TITLE_KEY = "title", RANGE_KEY = "range", VOLUME_KEY = "volume", ID_KEY = "id";
    private DiscDataUtil() {}
    public static boolean isMusicDisc(ItemStack stack) { return !stack.isEmpty() && stack.get(DataComponents.JUKEBOX_PLAYABLE) != null; }
    public static boolean hasCustomDisc(ItemStack stack) { return read(stack).isPresent(); }
    public static Optional<CustomDiscData> read(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA); if (data == null) return Optional.empty();
        CompoundTag root = data.copyTag(); if (!root.contains(ROOT_KEY)) return Optional.empty();
        CompoundTag tag = root.getCompound(ROOT_KEY).orElse(new CompoundTag());
        if (!tag.contains(URL_KEY) || !tag.contains(ID_KEY)) return Optional.empty();
        try {
            String url = tag.getString(URL_KEY).orElse(""), title = tag.getString(TITLE_KEY).orElse(url);
            int range = tag.getInt(RANGE_KEY).orElse(64); float volume = tag.getFloat(VOLUME_KEY).orElse(1.0F);
            UUID id = UUID.fromString(tag.getString(ID_KEY).orElse(""));
            return Optional.of(new CustomDiscData(url, title, range, volume, id));
        } catch (Exception e) { return Optional.empty(); }
    }
    public static void write(ItemStack stack, CustomDiscData disc) {
        CompoundTag root = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = new CompoundTag();
        tag.putString(URL_KEY, disc.url()); tag.putString(TITLE_KEY, disc.title());
        tag.putInt(RANGE_KEY, disc.range()); tag.putFloat(VOLUME_KEY, disc.volume()); tag.putString(ID_KEY, disc.id().toString());
        root.put(ROOT_KEY, tag); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    public static void clear(ItemStack stack) {
        CustomData old = stack.get(DataComponents.CUSTOM_DATA); if (old == null) return;
        CompoundTag root = old.copyTag(); root.remove(ROOT_KEY);
        if (root.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA); else stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
