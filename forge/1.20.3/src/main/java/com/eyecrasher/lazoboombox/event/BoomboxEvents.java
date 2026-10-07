package com.eyecrasher.lazoboombox.event;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import com.eyecrasher.lazoboombox.voice.LazoBoomboxServerBootstrap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class BoomboxEvents {
    private BoomboxEvents() {}

    public static void register() {
        // Registration happens via MinecraftForge.EVENT_BUS.register(BoomboxEvents.class) in
        // LazoBoombox
        // constructor
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
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            BoomboxPlaybackManager.INSTANCE.tickPlayer(player);
            if (event.getServer().getTickCount() % 4 == 0)
                BoomboxPlaybackManager.INSTANCE.tickHud(player);
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

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().isSpectator() || !event.getEntity().isShiftKeyDown()) return;
        if (event.getLevel().getBlockState(event.getPos()).getBlock()
                instanceof com.eyecrasher.lazoboombox.block.BoomboxBlock) {
            event.setUseBlock(Event.Result.ALLOW);
            event.setUseItem(Event.Result.DENY);
        }
    }
}
