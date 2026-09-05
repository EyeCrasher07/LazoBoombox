package com.eyecrasher.lazoboombox.item;
import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public final class ModCreativeTabs {
    private ModCreativeTabs() {}
    public static void initialize(IEventBus modBus) {
        modBus.addListener(ModCreativeTabs::onBuildCreativeTab);
    }
    private static void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.insertAfter(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.JUKEBOX),
                new net.minecraft.world.item.ItemStack(ModBlocks.BOOMBOX_ITEM.get()),
                net.minecraft.world.item.CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
        }
    }
}
