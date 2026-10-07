package com.eyecrasher.lazoboombox.client;

import com.eyecrasher.lazoboombox.block.BoomboxBlock;
import com.eyecrasher.lazoboombox.block.BoomboxBlockEntity;
import com.eyecrasher.lazoboombox.block.BoomboxOrientation;
import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;

/** Renders only intermediate angles; cardinal blocks retain ordinary chunk rendering. */
public final class BoomboxBlockEntityRenderer extends TileEntityRenderer<BoomboxBlockEntity> {
    private final BlockRendererDispatcher blocks;

    public BoomboxBlockEntityRenderer(
            net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher context) {
        super(context);
        blocks = net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(
            BoomboxBlockEntity entity,
            float partialTick,
            MatrixStack poses,
            IRenderTypeBuffer buffers,
            int light,
            int overlay) {
        BlockState state = entity.getBlockState();
        int offset = state.getValue(BoomboxBlock.ROTATION);
        if (offset == 0) return;
        BlockState cardinal = state.setValue(BoomboxBlock.ROTATION, 0);
        int color = entity.getColor();
        poses.pushPose();
        try {
            poses.translate(0.5, 0, 0.5);
            poses.mulPose(
                    net.minecraft.util.math.vector.Vector3f.YP.rotationDegrees(
                            -offset * BoomboxOrientation.DEGREES_PER_STEP));
            poses.translate(-0.5, 0, -0.5);
            blocks.getModelRenderer()
                    .renderModel(
                            poses.last(),
                            buffers.getBuffer(
                                    net.minecraft.client.renderer.Atlases.cutoutBlockSheet()),
                            cardinal,
                            blocks.getBlockModel(cardinal),
                            ((color >> 16) & 255) / 255.0F,
                            ((color >> 8) & 255) / 255.0F,
                            (color & 255) / 255.0F,
                            light,
                            overlay);
        } finally {
            poses.popPose();
        }
    }
}
