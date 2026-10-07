package com.eyecrasher.lazoboombox.item;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.block.ModBlocks;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
                .register(entries -> entries.addAfter(Items.JUKEBOX, ModBlocks.BOOMBOX_ITEM));
        LazoBoombox.LOGGER.info("Added LazoBoombox to functional_blocks tab");
    }
}
