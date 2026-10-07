package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.item.BoomboxItem;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LazoBoombox.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LazoBoombox.MOD_ID);
    private static final DeferredRegister<TileEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, LazoBoombox.MOD_ID);

    public static final Supplier<Block> BOOMBOX =
            BLOCKS.register(
                    "boombox",
                    () ->
                            new BoomboxBlock(
                                    AbstractBlock.Properties.of(Material.WOOD)
                                            .strength(1.5F, 6.0F)
                                            .noOcclusion()));
    public static final Supplier<BoomboxItem> BOOMBOX_ITEM =
            ITEMS.register(
                    "boombox",
                    () ->
                            new BoomboxItem(
                                    BOOMBOX.get(),
                                    new Item.Properties()
                                            .stacksTo(1)
                                            .fireResistant()
                                            .tab(net.minecraft.item.ItemGroup.TAB_DECORATIONS)));
    // The public builder avoids access transformers for block-entity registration.
    public static final Supplier<TileEntityType<BoomboxBlockEntity>> BOOMBOX_ENTITY =
            BLOCK_ENTITIES.register(
                    "boombox",
                    () ->
                            TileEntityType.Builder.of(BoomboxBlockEntity::new, BOOMBOX.get())
                                    .build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
