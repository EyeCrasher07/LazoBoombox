package com.eyecrasher.lazoboombox.server;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.compat.BoomboxSableCompat; import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.data.BoomboxData; import com.eyecrasher.lazoboombox.item.BoomboxItem; import com.eyecrasher.lazoboombox.network.BoomboxNetworking;
import com.eyecrasher.lazoboombox.voice.BoomboxAudioEngine; import com.eyecrasher.lazoboombox.voice.BoomboxPlayback;
import com.eyecrasher.lazodiscs.data.CustomDiscData; import com.eyecrasher.lazodiscs.data.DiscDataUtil;
import net.minecraft.core.BlockPos; import net.minecraft.core.GlobalPos; import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer; import net.minecraft.world.item.ItemStack; import net.minecraft.world.phys.Vec3;
import java.util.*; import java.util.concurrent.ConcurrentHashMap;
public final class BoomboxPlaybackManager {
    public static final BoomboxPlaybackManager INSTANCE = new BoomboxPlaybackManager();
    private static final class HeldEntry { final BoomboxPlayback playback; final UUID discId; HeldEntry(BoomboxPlayback p, UUID id) { playback = p; discId = id; } }
    private static final class PlacedEntry { final BoomboxPlayback playback; final UUID discId; PlacedEntry(BoomboxPlayback p, UUID id) { playback = p; discId = id; } }
    private final Map<UUID, HeldEntry> held = new ConcurrentHashMap<>();
    private final Map<GlobalPos, PlacedEntry> placed = new ConcurrentHashMap<>();
    private BoomboxPlaybackManager() {}
    public void tickPlayer(ServerPlayer player) {
        BoomboxHeldState state = readHeldBoombox(player);
        if (state == null) { stopHeld(player.getUUID(), "no-longer-held"); return; }
        HeldEntry existing = held.get(player.getUUID());
        if (existing != null && existing.discId.equals(state.disc.id())) return;
        stopHeld(player.getUUID(), "disc-changed"); startHeld(player, state.disc, state.boombox);
    }
    public void stopHeld(UUID uuid, String reason) { HeldEntry e = held.remove(uuid); if (e != null) try { e.playback.stop(); } catch (Throwable t) {} }
    private void startHeld(ServerPlayer player, CustomDiscData disc, ItemStack boombox) {
        if (!BoomboxAudioEngine.isReady()) return;
        try { BoomboxPlayback pb = BoomboxAudioEngine.startEntity(player, disc, () -> onHeldFinished(player)); held.put(player.getUUID(), new HeldEntry(pb, disc.id())); }
        catch (Throwable t) { player.sendSystemMessage(Component.translatable("lazoboombox.message.playback_failed", t.getMessage())); }
    }
    private void onHeldFinished(ServerPlayer player) { HeldEntry e = held.remove(player.getUUID()); if (e != null) try { e.playback.stop(); } catch (Throwable t) {} BoomboxNetworking.sendTimerState(player, false, 0L, 0L); }
    public void startPlaced(ServerLevel level, BlockPos pos, CustomDiscData disc) {
        if (!BoomboxConfig.ALLOW_PLAYBACK_ON_SABLE_PLATFORMS.get() && BoomboxSableCompat.isProbablySubLevel(pos)) {
            LazoBoombox.LOGGER.info("Refusing to start Boombox playback for '{}' at {} — block is on a Sable sub-level and allowPlaybackOnSablePlatforms is disabled in config.", disc.title(), pos.toShortString());
            return;
        }
        GlobalPos key = BoomboxSourceKey.placed(level, pos); PlacedEntry existing = placed.get(key);
        if (existing != null && disc.id().equals(existing.discId)) return;
        stopPlaced(level, pos, "restart"); if (!BoomboxAudioEngine.isReady()) return;
        try { BoomboxPlayback pb = BoomboxAudioEngine.start(level, Vec3.atCenterOf(pos), disc, () -> onPlacedFinished(level, pos)); placed.put(key, new PlacedEntry(pb, disc.id())); } catch (Throwable t) {}
    }
    public void updatePlacedPositionIfDynamic(ServerLevel level, BlockPos pos) {
        GlobalPos key = BoomboxSourceKey.placed(level, pos);
        PlacedEntry entry = placed.get(key);
        if (entry == null) return;
        BoomboxPlayback pb = entry.playback;
        if (!pb.isDynamicPosition()) {
            if (!BoomboxSableCompat.isProbablySubLevel(pos)) return;
            pb.markDynamicPosition();
            LazoBoombox.LOGGER.info("Placed boombox at {} is now on a Sable sub-level — switching to dynamic position tracking", pos.toShortString());
        }
        try {
            Vec3 projected = BoomboxSableCompat.projectBoomboxCenter(level, pos);
            pb.updatePosition(level, projected);
        } catch (Throwable t) {
            LazoBoombox.LOGGER.debug("Failed to update placed boombox position: {}", t.toString());
        }
    }

    public void onSablePostPhysicsTick(ServerLevel level) {
        if (placed.isEmpty()) return;
        for (Map.Entry<GlobalPos, PlacedEntry> e : placed.entrySet()) {
            GlobalPos gp = e.getKey();
            if (!gp.dimension().equals(level.dimension())) continue;
            PlacedEntry entry = e.getValue();
            BoomboxPlayback pb = entry.playback;
            if (!pb.isDynamicPosition()) {
                if (!BoomboxSableCompat.isProbablySubLevel(gp.pos())) continue;
                pb.markDynamicPosition();
            }
            try {
                Vec3 projected = BoomboxSableCompat.projectBoomboxCenter(level, gp.pos());
                pb.updatePosition(level, projected);
            } catch (Throwable t) {
                LazoBoombox.LOGGER.debug("SablePostPhysicsTick position update failed: {}", t.toString());
            }
        }
    }

    public void stopPlaced(ServerLevel level, BlockPos pos, String reason) { GlobalPos key = BoomboxSourceKey.placed(level, pos); PlacedEntry e = placed.remove(key); if (e != null) try { e.playback.stop(); } catch (Throwable t) {} }
    public boolean isPlacedActive(ServerLevel level, BlockPos pos) { return placed.containsKey(BoomboxSourceKey.placed(level, pos)); }
    private void onPlacedFinished(ServerLevel level, BlockPos pos) { GlobalPos key = BoomboxSourceKey.placed(level, pos); PlacedEntry e = placed.remove(key); if (e != null) try { e.playback.stop(); } catch (Throwable t) {} }
    public void stopAll(String reason) { for (HeldEntry e : held.values()) try { e.playback.stop(); } catch (Throwable t) {} held.clear(); for (PlacedEntry e : placed.values()) try { e.playback.stop(); } catch (Throwable t) {} placed.clear(); }
    public void stopChunk(ServerLevel level, net.minecraft.world.level.ChunkPos cp, String reason) { Iterator<Map.Entry<GlobalPos, PlacedEntry>> it = placed.entrySet().iterator(); while (it.hasNext()) { Map.Entry<GlobalPos, PlacedEntry> en = it.next(); GlobalPos gp = en.getKey(); if (gp.dimension().equals(level.dimension()) && cp.equals(new net.minecraft.world.level.ChunkPos(gp.pos()))) { try { en.getValue().playback.stop(); } catch (Throwable t) {} it.remove(); } } }
    public void tickHud(ServerPlayer player) { HeldEntry e = held.get(player.getUUID()); if (e == null || e.playback.isStopped()) { BoomboxNetworking.sendTimerState(player, false, 0L, 0L); return; } BoomboxNetworking.sendTimerState(player, true, e.playback.positionMs(), e.playback.durationMs()); }
    private static final class BoomboxHeldState { final ItemStack boombox; final CustomDiscData disc; BoomboxHeldState(ItemStack b, CustomDiscData d) { boombox = b; disc = d; } }
    private BoomboxHeldState readHeldBoombox(ServerPlayer player) {
        ItemStack main = player.getMainHandItem(), off = player.getOffhandItem(), boombox = null;
        if (main.getItem() instanceof BoomboxItem) boombox = main; else if (off.getItem() instanceof BoomboxItem) boombox = off;
        if (boombox == null || !BoomboxData.hasDisc(boombox)) return null;
        ItemStack disc = BoomboxData.readDisc(boombox, player.registryAccess()); if (disc.isEmpty()) return null;
        CustomDiscData data = DiscDataUtil.read(disc).orElse(null); return data == null ? null : new BoomboxHeldState(boombox, data);
    }
}
