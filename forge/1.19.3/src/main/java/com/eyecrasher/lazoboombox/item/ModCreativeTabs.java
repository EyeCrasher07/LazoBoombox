package com.eyecrasher.lazoboombox.item;

import com.eyecrasher.lazoboombox.block.ModBlocks;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.CreativeModeTabEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}

    public static void initialize(IEventBus modBus) {
        modBus.addListener(ModCreativeTabs::onBuildCreativeTab);
    }

    private static void onBuildCreativeTab(CreativeModeTabEvent.BuildContents event) {
        if (event.getTab() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.getEntries()
                    .putAfter(
                            new net.minecraft.world.item.ItemStack(
                                    net.minecraft.world.item.Items.JUKEBOX),
                            new net.minecraft.world.item.ItemStack(ModBlocks.BOOMBOX_ITEM.get()),
                            net.minecraft.world.item.CreativeModeTab.TabVisibility
                                    .PARENT_AND_SEARCH_TABS);
        }
    }
}
