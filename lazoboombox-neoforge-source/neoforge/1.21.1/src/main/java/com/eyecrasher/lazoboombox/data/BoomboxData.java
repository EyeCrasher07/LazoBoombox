package com.eyecrasher.lazoboombox.data;
import com.mojang.serialization.DataResult; import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents; import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps; import net.minecraft.nbt.Tag; import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack; import net.minecraft.world.item.component.CustomData;
import java.util.Optional; import java.util.UUID;
public final class BoomboxData {
    public static final String ROOT_KEY = "lazoboombox_boombox";
    private static final String DISC_KEY = "disc", OWNER_KEY = "owner";
    private BoomboxData() {}
    private static RegistryOps<Tag> ops(HolderLookup.Provider r) { return RegistryOps.create(NbtOps.INSTANCE, r); }
    public static boolean hasDisc(ItemStack b) {
        if (b.isEmpty()) return false; CustomData d = b.get(DataComponents.CUSTOM_DATA); if (d == null) return false;
        CompoundTag root = d.copyTag(); if (!root.contains(ROOT_KEY)) return false;
        return root.getCompound(ROOT_KEY).contains(DISC_KEY);
    }
    public static ItemStack readDisc(ItemStack b, HolderLookup.Provider r) {
        if (b.isEmpty()) return ItemStack.EMPTY; CustomData d = b.get(DataComponents.CUSTOM_DATA); if (d == null) return ItemStack.EMPTY;
        CompoundTag root = d.copyTag(); if (!root.contains(ROOT_KEY)) return ItemStack.EMPTY;
        CompoundTag tag = root.getCompound(ROOT_KEY);
        net.minecraft.nbt.Tag discTag = tag.get(DISC_KEY); if (discTag == null) return ItemStack.EMPTY;
        return ItemStack.CODEC.parse(ops(r), discTag).result().orElse(ItemStack.EMPTY);
    }
    public static void writeDisc(ItemStack b, ItemStack disc, HolderLookup.Provider r) {
        if (b.isEmpty()) return; CompoundTag root = b.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = root.getCompound(ROOT_KEY);
        if (disc.isEmpty()) { tag.remove(DISC_KEY); } else { DataResult<Tag> res = ItemStack.CODEC.encodeStart(ops(r), disc); res.result().ifPresent(t -> tag.put(DISC_KEY, t)); }
        root.put(ROOT_KEY, tag); b.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    public static void clearDisc(ItemStack b) {
        if (b.isEmpty()) return; CustomData old = b.get(DataComponents.CUSTOM_DATA); if (old == null) return;
        CompoundTag root = old.copyTag(); if (!root.contains(ROOT_KEY)) return;
        CompoundTag tag = root.getCompound(ROOT_KEY); tag.remove(DISC_KEY);
        if (tag.isEmpty()) root.remove(ROOT_KEY); else root.put(ROOT_KEY, tag);
        if (root.isEmpty()) b.remove(DataComponents.CUSTOM_DATA); else b.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
    public static Optional<UUID> readOwner(ItemStack b) {
        if (b.isEmpty()) return Optional.empty(); CustomData d = b.get(DataComponents.CUSTOM_DATA); if (d == null) return Optional.empty();
        CompoundTag root = d.copyTag(); if (!root.contains(ROOT_KEY)) return Optional.empty();
        CompoundTag tag = root.getCompound(ROOT_KEY);
        String s = tag.getString(OWNER_KEY); if (s == null || s.isEmpty()) return Optional.empty();
        try { return Optional.of(UUID.fromString(s)); } catch (Exception e) { return Optional.empty(); }
    }
    public static void writeOwner(ItemStack b, UUID owner) {
        if (b.isEmpty()) return; CompoundTag root = b.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag tag = root.getCompound(ROOT_KEY);
        if (owner == null) tag.remove(OWNER_KEY); else tag.putString(OWNER_KEY, owner.toString());
        root.put(ROOT_KEY, tag); b.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
