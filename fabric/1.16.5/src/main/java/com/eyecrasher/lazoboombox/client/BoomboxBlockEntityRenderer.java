package com.eyecrasher.lazoboombox.client;

import com.eyecrasher.lazoboombox.block.BoomboxBlock;
import com.eyecrasher.lazoboombox.block.BoomboxBlockEntity;
import com.eyecrasher.lazoboombox.block.BoomboxOrientation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.state.BlockState;

/** Renders only intermediate angles; cardinal blocks retain ordinary chunk rendering. */
public final class BoomboxBlockEntityRenderer extends BlockEntityRenderer<BoomboxBlockEntity> {
    private final BlockRenderDispatcher blocks;

    public BoomboxBlockEntityRenderer(
            net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher context) {
        super(context);
        blocks = net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(
            BoomboxBlockEntity entity,
            float partialTick,
            PoseStack poses,
            MultiBufferSource buffers,
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
                    com.mojang.math.Vector3f.YP.rotationDegrees(
                            -offset * BoomboxOrientation.DEGREES_PER_STEP));
            poses.translate(-0.5, 0, -0.5);
            blocks.getModelRenderer()
                    .renderModel(
                            poses.last(),
                            buffers.getBuffer(
                                    net.minecraft.client.renderer.Sheets.cutoutBlockSheet()),
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
