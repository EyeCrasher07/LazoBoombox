package com.eyecrasher.lazoboombox.block;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.item.BoomboxItem;
import net.minecraft.core.Registry; import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation; import net.minecraft.resources.ResourceKey; import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block; import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour; import net.minecraft.world.level.material.MapColor;
public final class ModBlocks {
    public static Block BOOMBOX; public static BoomboxItem BOOMBOX_ITEM; public static BlockEntityType<BoomboxBlockEntity> BOOMBOX_ENTITY;
    private ModBlocks() {}
    public static void initialize() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(LazoBoombox.MOD_ID, "boombox");
                ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), id);
        BOOMBOX = Registry.register(BuiltInRegistries.BLOCK, id, new BoomboxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F, 6.0F).noOcclusion().setId(blockKey)));
                ResourceKey<Item> itemKey = ResourceKey.create(BuiltInRegistries.ITEM.key(), id);
        BOOMBOX_ITEM = Registry.register(BuiltInRegistries.ITEM, id, new BoomboxItem(BOOMBOX, new Item.Properties().setId(itemKey).stacksTo(1).fireResistant()));
        BOOMBOX_ENTITY = BlockEntityType.register(LazoBoombox.MOD_ID + ":boombox", BoomboxBlockEntity::new, BOOMBOX);
        LazoBoombox.LOGGER.info("Registered LazoBoombox block + item + block-entity");
    }
}
