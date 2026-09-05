package com.eyecrasher.lazoboombox.event;
import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazoboombox.voice.LazoBoomboxServerBootstrap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class BoomboxEvents {
    private BoomboxEvents() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
            LazoBoombox.setCurrentServer(server);
            LazoBoomboxServerBootstrap.loadPlasmoAddon();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register((server) ->
            BoomboxPlaybackManager.INSTANCE.stopAll("server-stopping"));
        ServerLifecycleEvents.SERVER_STOPPED.register((server) ->
            LazoBoombox.setCurrentServer(null));

        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BoomboxPlaybackManager.INSTANCE.tickPlayer(player);
                if (server.getTickCount() % 4 == 0) BoomboxPlaybackManager.INSTANCE.tickHud(player);
            }
        });

        // NOTE: In Fabric, Block.useItemOn is called BEFORE Item.useOn, and shift does NOT
        // suppress the block. So BoomboxBlock.smartInteract (insert/swap/extract/pickup) runs
        // correctly for Shift+RMB with a disc in hand — no event override needed.
    }
}
