package com.eyecrasher.lazoboombox.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Per-entity snapshot for intermediate-angle block rendering. */
public final class BoomboxRenderState extends BlockEntityRenderState {
    public int offset;
    public int color;
    public final net.minecraft.client.renderer.block.BlockModelRenderState model =
            new net.minecraft.client.renderer.block.BlockModelRenderState();
    public net.minecraft.client.renderer.block.dispatch.BlockStateModel breakingModel;
}
