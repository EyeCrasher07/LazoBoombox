package com.eyecrasher.lazoboombox.event;
import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazoboombox.voice.LazoBoomboxServerBootstrap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class BoomboxEvents {
    private BoomboxEvents() {}

    public static void register() {
        // Registration happens via NeoForge.EVENT_BUS.register(BoomboxEvents.class) in LazoBoombox constructor
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        LazoBoombox.setCurrentServer(event.getServer());
        LazoBoomboxServerBootstrap.loadPlasmoAddon();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        BoomboxPlaybackManager.INSTANCE.stopAll("server-stopping");
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        LazoBoombox.setCurrentServer(null);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            BoomboxPlaybackManager.INSTANCE.tickPlayer(player);
            if (event.getServer().getTickCount() % 4 == 0) BoomboxPlaybackManager.INSTANCE.tickHud(player);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        BoomboxPlaybackManager.INSTANCE.stopChunk(level, event.getChunk().getPos(), "chunk-unload");
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp)
            BoomboxPlaybackManager.INSTANCE.stopHeld(sp.getUUID(), "player-death");
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp)
            BoomboxPlaybackManager.INSTANCE.stopHeld(sp.getUUID(), "player-disconnect");
    }
}
