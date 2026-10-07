package com.eyecrasher.lazoboombox.client;

import com.eyecrasher.lazoboombox.block.BoomboxBlock;
import com.eyecrasher.lazoboombox.block.BoomboxBlockEntity;
import com.eyecrasher.lazoboombox.block.BoomboxOrientation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Extracts color and geometry before submission; rendering never reads a live block entity. */
public final class BoomboxBlockEntityRenderer
        implements BlockEntityRenderer<BoomboxBlockEntity, BoomboxRenderState> {
    private final BlockModelResolver blocks;
    private static final net.minecraft.client.renderer.block.model.BlockDisplayContext
            DISPLAY_CONTEXT =
                    net.minecraft.client.renderer.block.model.BlockDisplayContext.create();

    public BoomboxBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.blockModelResolver();
    }

    @Override
    public BoomboxRenderState createRenderState() {
        return new BoomboxRenderState();
    }

    @Override
    public void extractRenderState(
            BoomboxBlockEntity entity,
            BoomboxRenderState state,
            float partialTick,
            Vec3 cameraPos,
            ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(
                entity, state, partialTick, cameraPos, breaking);
        state.offset = entity.getBlockState().getValue(BoomboxBlock.ROTATION);
        state.color = 0xFF000000 | entity.getColor();
        state.model.clear();
        if (state.offset != 0) {
            blocks.update(
                    state.model,
                    entity.getBlockState().setValue(BoomboxBlock.ROTATION, 0),
                    DISPLAY_CONTEXT);
            state.model.tintLayers().clear();
            state.model.tintLayers().add(state.color);
            state.breakingParts = java.util.List.of();
            if (breaking != null) {
                java.util.List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart>
                        parts = new java.util.ArrayList<>();
                net.minecraft.client.Minecraft.getInstance()
                        .getModelManager()
                        .getBlockStateModelSet()
                        .get(entity.getBlockState().setValue(BoomboxBlock.ROTATION, 0))
                        .collectParts(net.minecraft.util.RandomSource.create(42L), parts);
                state.breakingParts = java.util.List.copyOf(parts);
            }
        }
    }

    @Override
    public void submit(
            BoomboxRenderState state,
            PoseStack poses,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.offset == 0 || state.model.isEmpty()) return;
        poses.pushPose();
        try {
            poses.translate(0.5, 0, 0.5);
            poses.mulPose(
                    Axis.YP.rotationDegrees(-state.offset * BoomboxOrientation.DEGREES_PER_STEP));
            poses.translate(-0.5, 0, -0.5);
            state.model.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            if (state.breakProgress != null)
                collector.submitBreakingBlockModel(
                        poses, state.breakingParts, state.breakProgress.progress());
        } finally {
            poses.popPose();
        }
    }
}
