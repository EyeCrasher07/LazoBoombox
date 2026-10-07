package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.item.BoomboxItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, LazoBoombox.MOD_ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, LazoBoombox.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, LazoBoombox.MOD_ID);

    public static final Supplier<Block> BOOMBOX =
            BLOCKS.register(
                    "boombox",
                    () ->
                            new BoomboxBlock(
                                    BlockBehaviour.Properties.of(Material.WOOD)
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
                                            .tab(
                                                    net.minecraft.world.item.CreativeModeTab
                                                            .TAB_DECORATIONS)));
    public static final Supplier<BlockEntityType<BoomboxBlockEntity>> BOOMBOX_ENTITY =
            BLOCK_ENTITIES.register(
                    "boombox",
                    () ->
                            BlockEntityType.Builder.of(BoomboxBlockEntity::new, BOOMBOX.get())
                                    .build(null));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
