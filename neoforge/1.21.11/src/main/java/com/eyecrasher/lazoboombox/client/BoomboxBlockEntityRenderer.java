package com.eyecrasher.lazoboombox.client;

import com.eyecrasher.lazoboombox.block.BoomboxBlock;
import com.eyecrasher.lazoboombox.block.BoomboxBlockEntity;
import com.eyecrasher.lazoboombox.block.BoomboxOrientation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Extracts color and geometry before submission; rendering never reads a live block entity. */
public final class BoomboxBlockEntityRenderer
        implements BlockEntityRenderer<BoomboxBlockEntity, BoomboxRenderState> {
    private final BlockRenderDispatcher blocks;

    public BoomboxBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.blockRenderDispatcher();
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
        state.model =
                state.offset == 0
                        ? null
                        : blocks.getBlockModel(
                                entity.getBlockState().setValue(BoomboxBlock.ROTATION, 0));
    }

    @Override
    public void submit(
            BoomboxRenderState state,
            PoseStack poses,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.offset == 0 || state.model == null) return;
        poses.pushPose();
        try {
            poses.translate(0.5, 0, 0.5);
            poses.mulPose(
                    Axis.YP.rotationDegrees(-state.offset * BoomboxOrientation.DEGREES_PER_STEP));
            poses.translate(-0.5, 0, -0.5);
            collector.submitBlockModel(
                    poses,
                    net.minecraft.client.renderer.Sheets.cutoutBlockSheet(),
                    state.model,
                    ((state.color >> 16) & 255) / 255.0F,
                    ((state.color >> 8) & 255) / 255.0F,
                    (state.color & 255) / 255.0F,
                    state.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    0);
            if (state.breakProgress != null) {
                net.minecraft.client.renderer.block.model.BlockStateModel model = state.model;
                int light = state.lightCoords;
                collector.submitCustomGeometry(
                        poses,
                        net.minecraft.client.resources.model.ModelBakery.DESTROY_TYPES.get(
                                state.breakProgress.progress()),
                        (pose, vertices) ->
                                net.minecraft.client.renderer.block.ModelBlockRenderer.renderModel(
                                        pose,
                                        new com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator(
                                                vertices, pose, 1.0F),
                                        model,
                                        1.0F,
                                        1.0F,
                                        1.0F,
                                        light,
                                        OverlayTexture.NO_OVERLAY));
            }
        } finally {
            poses.popPose();
        }
    }
}
