package com.eyecrasher.lazoboombox.data;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.registry.DynamicRegistries;

import java.util.Optional;
import java.util.UUID;

public final class BoomboxData {
    public static final String ROOT_KEY = "lazoboombox_boombox";
    private static final String DISC_KEY = "disc", OWNER_KEY = "owner";

    public static final int UNPAINTED = -1;
    private static final String COLOR_KEY = "color";

    public static boolean isValidColor(int color) {
        return color >= 0 && color <= 0xFFFFFF;
    }

    /** Missing or malformed color leaves existing boomboxes visually unchanged. */
    public static int readColor(ItemStack b) {
        if (b.isEmpty()) return UNPAINTED;
        CompoundNBT root = b.getTag();
        if (root == null) return UNPAINTED;
        CompoundNBT tag = root.getCompound(ROOT_KEY);
        int color = tag.contains(COLOR_KEY, 3) ? tag.getInt(COLOR_KEY) : UNPAINTED;
        return isValidColor(color) ? color : UNPAINTED;
    }

    /** Copy-on-write preserves the disc, owner, and all unrelated item metadata. */
    public static void writeColor(ItemStack b, int color) {
        if (b.isEmpty()) return;
        if (color != UNPAINTED && !isValidColor(color))
            throw new IllegalArgumentException("Color must be a 24-bit RGB value or UNPAINTED");
        CompoundNBT root = b.getTag() == null ? new CompoundNBT() : b.getTag().copy();
        CompoundNBT tag = root.getCompound(ROOT_KEY);
        if (color == UNPAINTED) tag.remove(COLOR_KEY);
        else tag.putInt(COLOR_KEY, color);
        if (tag.isEmpty()) root.remove(ROOT_KEY);
        else root.put(ROOT_KEY, tag);
        b.setTag(root.isEmpty() ? null : root);
    }

    private BoomboxData() {}

    public static boolean hasDisc(ItemStack b) {
        if (b.isEmpty()) return false;
        CompoundNBT root = b.getTag();
        if (root == null) return false;
        if (!root.contains(ROOT_KEY)) return false;
        return root.getCompound(ROOT_KEY).contains(DISC_KEY);
    }

    public static ItemStack readDisc(ItemStack b, DynamicRegistries r) {
        if (b.isEmpty()) return ItemStack.EMPTY;
        CompoundNBT root = b.getTag();
        if (root == null) return ItemStack.EMPTY;
        if (!root.contains(ROOT_KEY)) return ItemStack.EMPTY;
        CompoundNBT tag = root.getCompound(ROOT_KEY);
        if (!tag.contains(DISC_KEY, 10)) return ItemStack.EMPTY;
        return ItemStack.of(tag.getCompound(DISC_KEY));
    }

    public static void writeDisc(ItemStack b, ItemStack disc, DynamicRegistries r) {
        if (b.isEmpty()) return;
        CompoundNBT root = b.getTag() == null ? new CompoundNBT() : b.getTag().copy();
        CompoundNBT tag = root.getCompound(ROOT_KEY);
        if (disc.isEmpty()) {
            tag.remove(DISC_KEY);
        } else {
            tag.put(DISC_KEY, disc.save(new CompoundNBT()));
        }
        root.put(ROOT_KEY, tag);
        b.setTag(root);
    }

    public static void clearDisc(ItemStack b) {
        if (b.isEmpty()) return;
        CompoundNBT old = b.getTag();
        if (old == null) return;
        CompoundNBT root = old.copy();
        if (!root.contains(ROOT_KEY)) return;
        CompoundNBT tag = root.getCompound(ROOT_KEY);
        tag.remove(DISC_KEY);
        if (tag.isEmpty()) root.remove(ROOT_KEY);
        else root.put(ROOT_KEY, tag);
        b.setTag(root.isEmpty() ? null : root);
    }

    public static Optional<UUID> readOwner(ItemStack b) {
        if (b.isEmpty()) return Optional.empty();
        CompoundNBT root = b.getTag();
        if (root == null) return Optional.empty();
        if (!root.contains(ROOT_KEY)) return Optional.empty();
        CompoundNBT tag = root.getCompound(ROOT_KEY);
        String s = tag.getString(OWNER_KEY);
        if (s == null || s.isEmpty()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(s));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void writeOwner(ItemStack b, UUID owner) {
        if (b.isEmpty()) return;
        CompoundNBT root = b.getTag() == null ? new CompoundNBT() : b.getTag().copy();
        CompoundNBT tag = root.getCompound(ROOT_KEY);
        if (owner == null) tag.remove(OWNER_KEY);
        else tag.putString(OWNER_KEY, owner.toString());
        root.put(ROOT_KEY, tag);
        b.setTag(root);
    }
}
