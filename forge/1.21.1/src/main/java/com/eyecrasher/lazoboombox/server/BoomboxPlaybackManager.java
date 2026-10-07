package com.eyecrasher.lazoboombox.server;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.compat.BoomboxSableCompat;
import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.network.BoomboxNetworking;
import com.eyecrasher.lazoboombox.voice.BoomboxAudioEngine;
import com.eyecrasher.lazoboombox.voice.BoomboxPlayback;
import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.data.DiscDataUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BoomboxPlaybackManager {
    public static final BoomboxPlaybackManager INSTANCE = new BoomboxPlaybackManager();

    private static final class HeldEntry {
        final BoomboxPlayback playback;
        final CustomDiscData disc;
        final UUID sessionId;

        HeldEntry(BoomboxPlayback p, CustomDiscData disc, UUID session) {
            playback = p;
            this.disc = disc;
            sessionId = session;
        }
    }

    private static final class PlacedEntry {
        final BoomboxPlayback playback;
        final CustomDiscData disc;
        final UUID sessionId;

        PlacedEntry(BoomboxPlayback p, CustomDiscData disc, UUID session) {
            playback = p;
            this.disc = disc;
            sessionId = session;
        }
    }

    private final Map<UUID, HeldEntry> held = new ConcurrentHashMap<>();
    private final Map<GlobalPos, PlacedEntry> placed = new ConcurrentHashMap<>();

    private record RetryEntry(CustomDiscData disc, long retryAfterNanos) {}

    private static final long RETRY_DELAY_NANOS = java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
    private final Map<UUID, RetryEntry> heldRetries = new ConcurrentHashMap<>();
    private final Map<GlobalPos, RetryEntry> placedRetries = new ConcurrentHashMap<>();

    private BoomboxPlaybackManager() {}

    private static <K> boolean waitingForRetry(
            Map<K, RetryEntry> retries, K key, CustomDiscData disc) {
        RetryEntry retry = retries.get(key);
        if (retry == null) return false;
        if (!retry.disc().equals(disc)) {
            retries.remove(key, retry);
            return false;
        }
        return System.nanoTime() - retry.retryAfterNanos() < 0;
    }

    private static <K> boolean recordFailure(
            Map<K, RetryEntry> retries, K key, CustomDiscData disc) {
        RetryEntry previous =
                retries.put(key, new RetryEntry(disc, System.nanoTime() + RETRY_DELAY_NANOS));
        return previous == null || !previous.disc().equals(disc);
    }

    private static void stopPlayback(BoomboxPlayback playback, String reason) {
        try {
            playback.stop();
        } catch (RuntimeException error) {
            LazoBoombox.LOGGER.warn(
                    "Could not stop boombox playback ({}): {}", reason, error.toString());
        }
    }

    private void removeHeld(UUID uuid, String reason) {
        HeldEntry entry = held.remove(uuid);
        if (entry != null) stopPlayback(entry.playback, reason);
    }

    private void removePlaced(GlobalPos key, String reason) {
        PlacedEntry entry = placed.remove(key);
        if (entry != null) stopPlayback(entry.playback, reason);
    }

    public void tickPlayer(ServerPlayer player) {
        if (!player.isAlive()) {
            stopHeld(player.getUUID(), "player-death");
            return;
        }
        BoomboxHeldState state = readHeldBoombox(player);
        if (state == null) {
            stopHeld(player.getUUID(), "no-longer-held");
            return;
        }
        HeldEntry existing = held.get(player.getUUID());
        if (existing != null && existing.disc.equals(state.disc)) return;
        if (waitingForRetry(heldRetries, player.getUUID(), state.disc)) return;
        removeHeld(player.getUUID(), "disc-changed");
        startHeld(player, state.disc);
    }

    public void stopHeld(UUID uuid, String reason) {
        heldRetries.remove(uuid);
        removeHeld(uuid, reason);
    }

    private void startHeld(ServerPlayer player, CustomDiscData disc) {
        if (!BoomboxAudioEngine.isReady()) return;
        UUID sessionId = UUID.randomUUID();
        try {
            BoomboxPlayback pb =
                    BoomboxAudioEngine.startEntity(
                            player, disc, () -> onHeldFinished(player, sessionId));
            held.put(player.getUUID(), new HeldEntry(pb, disc, sessionId));
        } catch (RuntimeException error) {
            if (recordFailure(heldRetries, player.getUUID(), disc))
                player.sendSystemMessage(
                        Component.translatable(
                                "lazoboombox.message.playback_failed", error.getMessage()));
        }
    }

    private void onHeldFinished(ServerPlayer player, UUID sessionId) {
        HeldEntry e = held.get(player.getUUID());
        if (e == null || !sessionId.equals(e.sessionId) || !held.remove(player.getUUID(), e))
            return;
        if (e.playback.hasFailed()) {
            if (recordFailure(heldRetries, player.getUUID(), e.disc))
                player.sendSystemMessage(
                        Component.translatable(
                                "lazoboombox.message.playback_failed",
                                e.playback.failureMessage()));
        } else heldRetries.remove(player.getUUID());
        stopPlayback(e.playback, "finished");
        BoomboxNetworking.sendTimerState(player, false, 0L, 0L);
    }

    public void startPlaced(ServerLevel level, BlockPos pos, CustomDiscData disc) {
        if (!BoomboxConfig.FEATURE_PLACED_BOOMBOX.get()) return;
        // Block playback on Sable moving platforms unless explicitly enabled in config.
        // Same gate as LazoDiscs.JukeboxPlaybackManager.start() — keeps the two mods
        // consistent. Admin can enable in config/lazoboombox/config.toml.
        if (!BoomboxConfig.ALLOW_PLAYBACK_ON_SABLE_PLATFORMS.get()
                && BoomboxSableCompat.isProbablySubLevel(pos)) {
            LazoBoombox.LOGGER.debug(
                    "Refusing to start Boombox playback for '{}' at {} — block is on a Sable"
                        + " sub-level and allowPlaybackOnSablePlatforms is disabled in config."
                        + " Enable it in config/lazoboombox/config.toml to allow playback on moving"
                        + " platforms.",
                    disc.title(),
                    pos.toShortString());
            return;
        }

        GlobalPos key = BoomboxSourceKey.placed(level, pos);
        PlacedEntry existing = placed.get(key);
        if (existing != null && disc.equals(existing.disc)) return;
        if (waitingForRetry(placedRetries, key, disc)) return;
        removePlaced(key, "restart");
        if (!BoomboxAudioEngine.isReady()) return;
        UUID sessionId = UUID.randomUUID();
        try {
            BoomboxPlayback pb =
                    BoomboxAudioEngine.start(
                            level,
                            Vec3.atCenterOf(pos),
                            disc,
                            () -> onPlacedFinished(level, pos, sessionId));
            placed.put(key, new PlacedEntry(pb, disc, sessionId));
        } catch (RuntimeException error) {
            if (recordFailure(placedRetries, key, disc))
                LazoBoombox.LOGGER.warn(
                        "Could not start boombox playback at {}: {}",
                        pos.toShortString(),
                        error.toString());
        }
    }

    public void stopPlaced(ServerLevel level, BlockPos pos, String reason) {
        GlobalPos key = BoomboxSourceKey.placed(level, pos);
        placedRetries.remove(key);
        removePlaced(key, reason);
    }

    public boolean isPlacedActive(ServerLevel level, BlockPos pos) {
        return placed.containsKey(BoomboxSourceKey.placed(level, pos));
    }

    private void onPlacedFinished(ServerLevel level, BlockPos pos, UUID sessionId) {
        GlobalPos key = BoomboxSourceKey.placed(level, pos);
        PlacedEntry e = placed.get(key);
        if (e == null || !sessionId.equals(e.sessionId) || !placed.remove(key, e)) return;
        if (e.playback.hasFailed()) recordFailure(placedRetries, key, e.disc);
        else placedRetries.remove(key);
        stopPlayback(e.playback, "finished");
    }

    public void stopAll(String reason) {
        for (HeldEntry e : held.values()) stopPlayback(e.playback, reason);
        held.clear();
        for (PlacedEntry e : placed.values()) stopPlayback(e.playback, reason);
        placed.clear();
        heldRetries.clear();
        placedRetries.clear();
    }

    public void stopChunk(ServerLevel level, net.minecraft.world.level.ChunkPos cp, String reason) {
        placed.forEach(
                (key, entry) -> {
                    if (key.dimension().equals(level.dimension())
                            && cp.equals(new net.minecraft.world.level.ChunkPos(key.pos()))
                            && placed.remove(key, entry)) stopPlayback(entry.playback, reason);
                });
        placedRetries
                .keySet()
                .removeIf(
                        key ->
                                key.dimension().equals(level.dimension())
                                        && cp.equals(
                                                new net.minecraft.world.level.ChunkPos(key.pos())));
    }

    public void tickHud(ServerPlayer player) {
        HeldEntry e = held.get(player.getUUID());
        if (e == null || e.playback.isStopped()) {
            BoomboxNetworking.sendTimerState(player, false, 0L, 0L);
            return;
        }
        BoomboxNetworking.sendTimerState(
                player, true, e.playback.positionMs(), e.playback.durationMs());
    }

    private record BoomboxHeldState(CustomDiscData disc) {}

    private BoomboxHeldState readHeldBoombox(ServerPlayer player) {
        for (ItemStack boombox :
                new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
            if (!(boombox.getItem() == ModBlocks.BOOMBOX_ITEM.get())
                    || !BoomboxData.hasDisc(boombox)) continue;
            ItemStack disc = BoomboxData.readDisc(boombox, player.registryAccess());
            if (disc.isEmpty()) continue;
            CustomDiscData data = DiscDataUtil.read(disc).orElse(null);
            if (data != null) return new BoomboxHeldState(data);
        }
        return null;
    }

    /**
     * If the placed boombox at the given position is on a Sable moving platform, project its
     * current position to real-world coordinates and update the underlying Plasmo static source.
     *
     * <p>This is the Sable compatibility fix for the placed boombox. The method is a no-op if:
     *
     * <ul>
     *   <li>the placed boombox has no active playback (e.g. disc was removed);
     *   <li>Sable is not installed;
     *   <li>the placed boombox is NOT on a moving platform (its source was created with {@code
     *       dynamicPosition=false}).
     * </ul>
     *
     * <p>Called from two paths:
     *
     * <ol>
     *   <li>{@link com.eyecrasher.lazoboombox.block.BoomboxBlockEntity#serverTick} — every 20 ticks
     *       as a fallback;
     *   <li>{@link com.eyecrasher.lazoboombox.event.BoomboxSableEvents#onPostPhysicsTick} — after
     *       every Sable physics step (more responsive for fast-moving platforms).
     * </ol>
     */
    public void updatePlacedPositionIfDynamic(ServerLevel level, BlockPos pos) {
        GlobalPos key = BoomboxSourceKey.placed(level, pos);
        PlacedEntry entry = placed.get(key);
        if (entry == null) return;
        BoomboxPlayback pb = entry.playback;
        // Re-check the cheap coordinate heuristic every call, not just once at start(). A
        // boombox can start playing as a perfectly normal, static block and only later get
        // swept into a Sable sub-level (player runs the Physics Assembler under it) — if we
        // only decided dynamicPosition once at start(), the source would stay frozen at its
        // original real-world coordinates forever while the platform flies off.
        if (!pb.isDynamicPosition()) {
            if (!BoomboxSableCompat.isProbablySubLevel(pos)) return;
            pb.markDynamicPosition();
            LazoBoombox.LOGGER.info(
                    "Placed boombox at {} is now on a Sable sub-level — switching to dynamic"
                            + " position tracking",
                    pos.toShortString());
        }
        try {
            Vec3 projected = BoomboxSableCompat.projectBoomboxCenter(level, pos);
            pb.updatePosition(level, projected);
        } catch (Throwable t) {
            LazoBoombox.LOGGER.debug(
                    "Failed to update placed boombox position for Sable platform at {}: {}",
                    pos.toShortString(),
                    t.toString());
        }
    }

    /**
     * Updates positions for ALL placed boomboxes on the given level that need dynamic position
     * updates (i.e. those on Sable moving platforms). Called from the SablePostPhysicsTickEvent
     * listener after Sable has updated all sub-level poses.
     */
    public void onSablePostPhysicsTick(ServerLevel level) {
        if (placed.isEmpty()) return;
        for (Map.Entry<GlobalPos, PlacedEntry> e : placed.entrySet()) {
            GlobalPos gp = e.getKey();
            if (!gp.dimension().equals(level.dimension())) continue;
            PlacedEntry entry = e.getValue();
            if (!entry.playback.isDynamicPosition()) {
                if (!BoomboxSableCompat.isProbablySubLevel(gp.pos())) continue;
                entry.playback.markDynamicPosition();
            }
            try {
                Vec3 projected = BoomboxSableCompat.projectBoomboxCenter(level, gp.pos());
                entry.playback.updatePosition(level, projected);
            } catch (Throwable t) {
                LazoBoombox.LOGGER.debug(
                        "SablePostPhysicsTick boombox position update failed at {}: {}",
                        gp.pos().toShortString(),
                        t.toString());
            }
        }
    }
}
