package com.eyecrasher.lazoboombox.client;

import com.eyecrasher.lazoboombox.block.BoomboxBlockEntity;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.data.BoomboxData;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;

/** Client-only color registration; terrain particles retain paint briefly after removal. */
public final class BoomboxColors {
    private BoomboxColors() {}

    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        bus.addListener(BoomboxColors::registerBlocks);
        bus.addListener(BoomboxColors::registerItems);
        bus.addListener(BoomboxColors::registerRenderer);
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
            net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(
                () ->
                        net.minecraftforge.fml.client.registry.ClientRegistry
                                .bindTileEntityRenderer(
                                        ModBlocks.BOOMBOX_ENTITY.get(),
                                        BoomboxBlockEntityRenderer::new));
    }

    public static int itemColor(ItemStack stack, int tintIndex) {
        return tintIndex == 0 ? 0xFF000000 | BoomboxData.readColor(stack) : -1;
    }

    public static int blockColor(
            BlockState state, IBlockDisplayReader world, BlockPos pos, int tintIndex) {
        if (tintIndex != 0 || world == null || pos == null) return -1;
        net.minecraft.tileentity.TileEntity entity = world.getBlockEntity(pos);
        int color =
                entity instanceof BoomboxBlockEntity boombox
                        ? boombox.getColor()
                        : com.eyecrasher.lazoboombox.data.BoomboxParticlePaint.read(
                                world, pos.asLong());
        return 0xFF000000 | color;
    }
}
