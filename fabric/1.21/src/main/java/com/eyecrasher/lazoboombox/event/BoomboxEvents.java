package com.eyecrasher.lazoboombox.event;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazoboombox.voice.LazoBoomboxServerBootstrap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public final class BoomboxEvents {
    private BoomboxEvents() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(
                (server) -> {
                    LazoBoombox.setCurrentServer(server);
                    LazoBoomboxServerBootstrap.loadPlasmoAddon();
                });
        ServerLifecycleEvents.SERVER_STOPPING.register(
                (server) -> BoomboxPlaybackManager.INSTANCE.stopAll("server-stopping"));
        ServerLifecycleEvents.SERVER_STOPPED.register(
                (server) -> LazoBoombox.setCurrentServer(null));

        ServerChunkEvents.CHUNK_UNLOAD.register(
                (level, chunk) ->
                        BoomboxPlaybackManager.INSTANCE.stopChunk(
                                level, chunk.getPos(), "chunk-unload"));
        ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) ->
                        BoomboxPlaybackManager.INSTANCE.stopHeld(
                                handler.player.getUUID(), "player-disconnect"));

        ServerTickEvents.END_SERVER_TICK.register(
                (server) -> {
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        BoomboxPlaybackManager.INSTANCE.tickPlayer(player);
                        if (server.getTickCount() % 4 == 0)
                            BoomboxPlaybackManager.INSTANCE.tickHud(player);
                    }
                });

        // Sneaking with an occupied hand normally skips block interaction in vanilla.
        UseBlockCallback.EVENT.register(
                (player, level, hand, hit) -> {
                    if (player.isSpectator()
                            || !player.isShiftKeyDown()
                            || !level.mayInteract(player, hit.getBlockPos()))
                        return InteractionResult.PASS;
                    if (!(level.getBlockState(hit.getBlockPos()).getBlock()
                            instanceof com.eyecrasher.lazoboombox.block.BoomboxBlock boombox))
                        return InteractionResult.PASS;
                    if (level instanceof ServerLevel sl)
                        boombox.pickup(sl, hit.getBlockPos(), player);
                    return InteractionResult.SUCCESS;
                });
    }
}
