package com.eyecrasher.lazoboombox.server;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.server.ServerWorld;

public final class BoomboxSourceKey {
    private BoomboxSourceKey() {}

    public static GlobalPos placed(ServerWorld level, BlockPos pos) {
        return GlobalPos.of(level.dimension(), pos.immutable());
    }
}
