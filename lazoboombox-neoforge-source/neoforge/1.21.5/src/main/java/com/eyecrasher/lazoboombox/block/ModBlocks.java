package com.eyecrasher.lazoboombox.block;
import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.item.BoomboxItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, LazoBoombox.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, LazoBoombox.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LazoBoombox.MOD_ID);

    // MC 1.21.5+ requires the block/item id to be set on Properties BEFORE the
    // constructor runs (BlockBehaviour reads the id during construction), otherwise
    // it throws "Block id not set" / "Trying to access unbound value: ResourceKey".
    public static final Supplier<Block> BOOMBOX = BLOCKS.register("boombox", () -> {
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(LazoBoombox.MOD_ID, "boombox"));
        return new BoomboxBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                .strength(1.5F, 6.0F).noOcclusion().setId(blockKey));
    });
    public static final Supplier<BoomboxItem> BOOMBOX_ITEM = ITEMS.register("boombox", () -> {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(LazoBoombox.MOD_ID, "boombox"));
        return new BoomboxItem(BOOMBOX.get(), new Item.Properties().setId(itemKey)
                .stacksTo(1).fireResistant());
    });
    public static final Supplier<BlockEntityType<BoomboxBlockEntity>> BOOMBOX_ENTITY = BLOCK_ENTITIES.register(
        "boombox", () -> new BlockEntityType<>(BoomboxBlockEntity::new, BOOMBOX.get())
);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
