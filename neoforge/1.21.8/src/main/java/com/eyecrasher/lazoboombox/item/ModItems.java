package com.eyecrasher.lazoboombox.item;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.LazoBoombox;
public final class ModItems {
    private ModItems() {}
    public static BoomboxItem getBoombox() { return ModBlocks.BOOMBOX_ITEM.get(); }
    public static void initialize() { LazoBoombox.LOGGER.info("LazoBoombox items ready"); }
}
