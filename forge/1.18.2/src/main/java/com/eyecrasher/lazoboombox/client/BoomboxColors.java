package com.eyecrasher.lazoboombox.client;

import com.eyecrasher.lazoboombox.block.BoomboxBlockEntity;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.data.BoomboxData;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Client-only color registration; terrain particles retain paint briefly after removal. */
public final class BoomboxColors {
    private BoomboxColors() {}

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener(BoomboxColors::registerRenderer);
        bus.addListener(BoomboxColors::registerBlocks);
        bus.addListener(BoomboxColors::registerItems);
    }

    private static void registerBlocks(
            net.minecraftforge.client.event.ColorHandlerEvent.Block event) {
        event.getBlockColors().register(BoomboxColors::blockColor, ModBlocks.BOOMBOX.get());
    }

    private static void registerItems(
            net.minecraftforge.client.event.ColorHandlerEvent.Item event) {
        event.getItemColors().register(BoomboxColors::itemColor, ModBlocks.BOOMBOX_ITEM.get());
    }

    private static void registerRenderer(
            net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlocks.BOOMBOX_ENTITY.get(), BoomboxBlockEntityRenderer::new);
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? 0xFF000000 | BoomboxData.readColor(stack) : -1;
    }

    public static int blockColor(
            BlockState state, BlockAndTintGetter world, BlockPos pos, int tintIndex) {
        if (tintIndex != 0 || world == null || pos == null) return -1;
        net.minecraft.world.level.block.entity.BlockEntity entity = world.getBlockEntity(pos);
        int color =
                entity instanceof BoomboxBlockEntity boombox
                        ? boombox.getColor()
                        : com.eyecrasher.lazoboombox.data.BoomboxParticlePaint.read(
                                world, pos.asLong());
        return 0xFF000000 | color;
    }
}
