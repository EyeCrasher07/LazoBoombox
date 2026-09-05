package com.eyecrasher.lazoboombox.data;
import com.eyecrasher.lazodiscs.data.CustomDiscData; import com.eyecrasher.lazodiscs.data.DiscDataUtil;
import net.minecraft.core.HolderLookup; import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack; import net.minecraft.world.item.JukeboxSong;
public final class DiscNameUtil {
    private DiscNameUtil() {}
    public static Component get(ItemStack stack, HolderLookup.Provider registries) {
        if (stack == null || stack.isEmpty()) return Component.literal("?");
        CustomDiscData data = DiscDataUtil.read(stack).orElse(null);
        if (data != null) return Component.literal(data.title());
        try { var song = JukeboxSong.fromStack(registries, stack); if (song.isPresent()) return song.get().value().description(); } catch (Throwable ignored) {}
        return stack.getHoverName();
    }
}
