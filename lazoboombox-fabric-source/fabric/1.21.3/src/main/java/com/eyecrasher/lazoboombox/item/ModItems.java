package com.eyecrasher.lazoboombox.item;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.block.ModBlocks;
public final class ModItems {
    private ModItems() {}
    public static BoomboxItem getBoombox() { return ModBlocks.BOOMBOX_ITEM; }
    public static void initialize() { LazoBoombox.LOGGER.info("LazoBoombox items ready"); }
}
