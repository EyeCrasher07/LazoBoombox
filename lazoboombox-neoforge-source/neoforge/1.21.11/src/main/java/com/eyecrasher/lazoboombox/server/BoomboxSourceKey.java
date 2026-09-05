package com.eyecrasher.lazoboombox.server;
import net.minecraft.core.BlockPos; import net.minecraft.core.GlobalPos; import net.minecraft.server.level.ServerLevel;
public final class BoomboxSourceKey {
    private BoomboxSourceKey() {}
    public static GlobalPos placed(ServerLevel level, BlockPos pos) { return GlobalPos.of(level.dimension(), pos.immutable()); }
}
