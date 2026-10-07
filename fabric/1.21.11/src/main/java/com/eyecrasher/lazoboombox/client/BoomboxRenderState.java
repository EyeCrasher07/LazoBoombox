package com.eyecrasher.lazoboombox.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

/** Per-entity snapshot for intermediate-angle block rendering. */
public final class BoomboxRenderState extends BlockEntityRenderState {
    public int offset;
    public int color;
    public net.minecraft.client.renderer.block.model.BlockStateModel model;
}
