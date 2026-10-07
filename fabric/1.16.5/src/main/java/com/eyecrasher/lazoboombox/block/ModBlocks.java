package com.eyecrasher.lazoboombox.block;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.item.BoomboxItem;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public final class ModBlocks {
    public static Block BOOMBOX;
    public static BoomboxItem BOOMBOX_ITEM;
    public static BlockEntityType<BoomboxBlockEntity> BOOMBOX_ENTITY;

    private ModBlocks() {}

    public static void initialize() {
        ResourceLocation id = new ResourceLocation(LazoBoombox.MOD_ID, "boombox");
        BOOMBOX =
                Registry.register(
                        Registry.BLOCK,
                        id,
                        new BoomboxBlock(
                                BlockBehaviour.Properties.of(Material.WOOD)
                                        .strength(1.5F, 6.0F)
                                        .noOcclusion()));
        BOOMBOX_ITEM =
                Registry.register(
                        Registry.ITEM,
                        id,
                        new BoomboxItem(
                                BOOMBOX,
                                new Item.Properties()
                                        .stacksTo(1)
                                        .fireResistant()
                                        .tab(
                                                net.minecraft.world.item.CreativeModeTab
                                                        .TAB_DECORATIONS)));
        BOOMBOX_ENTITY =
                Registry.register(
                        Registry.BLOCK_ENTITY_TYPE,
                        id,
                        BlockEntityType.Builder.of(BoomboxBlockEntity::new, BOOMBOX).build(null));
        LazoBoombox.LOGGER.info("Registered LazoBoombox block + item + block-entity");
    }
}
