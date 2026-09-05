package com.eyecrasher.lazoboombox.item;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents; import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier; import net.minecraft.resources.ResourceKey; import net.minecraft.world.item.CreativeModeTab; import net.minecraft.world.item.Items;
public final class ModCreativeTabs {
    public static final ResourceKey<CreativeModeTab> FUNCTIONAL_BLOCKS = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("minecraft", "functional_blocks"));
    private ModCreativeTabs() {}
    public static void initialize() { ItemGroupEvents.modifyEntriesEvent(FUNCTIONAL_BLOCKS).register(entries -> entries.addAfter(Items.JUKEBOX, ModBlocks.BOOMBOX_ITEM)); LazoBoombox.LOGGER.info("Added LazoBoombox to functional_blocks tab"); }
}
