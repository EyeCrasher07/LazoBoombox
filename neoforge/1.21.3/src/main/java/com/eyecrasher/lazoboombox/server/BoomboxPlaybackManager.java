package com.eyecrasher.lazoboombox.server;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.data.BoomboxData; import com.eyecrasher.lazoboombox.item.BoomboxItem;
import com.eyecrasher.lazoboombox.block.ModBlocks; import com.eyecrasher.lazoboombox.network.BoomboxNetworking;
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
        if (!canStartMore()) { player.sendSystemMessage(Component.translatable("lazoboombox.message.too_many_sources")); return; }
        if (!BoomboxAudioEngine.isReady()) return;
        try { BoomboxPlayback pb = BoomboxAudioEngine.startEntity(player, disc, () -> onHeldFinished(player)); held.put(player.getUUID(), new HeldEntry(pb, disc.id())); }
        catch (Throwable t) { player.sendSystemMessage(Component.translatable("lazoboombox.message.playback_failed", t.getMessage())); }
    }
    private void onHeldFinished(ServerPlayer player) { HeldEntry e = held.remove(player.getUUID()); if (e != null) try { e.playback.stop(); } catch (Throwable t) {} BoomboxNetworking.sendTimerState(player, false, 0L, 0L); }
    public void startPlaced(ServerLevel level, BlockPos pos, CustomDiscData disc) {
        GlobalPos key = BoomboxSourceKey.placed(level, pos); PlacedEntry existing = placed.get(key);
        if (existing != null && disc.id().equals(existing.discId)) return;
        stopPlaced(level, pos, "restart"); if (!canStartMore()) return; if (!BoomboxAudioEngine.isReady()) return;
        try { BoomboxPlayback pb = BoomboxAudioEngine.start(level, Vec3.atCenterOf(pos), disc, () -> onPlacedFinished(level, pos)); placed.put(key, new PlacedEntry(pb, disc.id())); } catch (Throwable t) {}
    }
    public void stopPlaced(ServerLevel level, BlockPos pos, String reason) { GlobalPos key = BoomboxSourceKey.placed(level, pos); PlacedEntry e = placed.remove(key); if (e != null) try { e.playback.stop(); } catch (Throwable t) {} }
    public boolean isPlacedActive(ServerLevel level, BlockPos pos) { return placed.containsKey(BoomboxSourceKey.placed(level, pos)); }
    private void onPlacedFinished(ServerLevel level, BlockPos pos) { GlobalPos key = BoomboxSourceKey.placed(level, pos); PlacedEntry e = placed.remove(key); if (e != null) try { e.playback.stop(); } catch (Throwable t) {} }
    public void stopAll(String reason) { for (HeldEntry e : held.values()) try { e.playback.stop(); } catch (Throwable t) {} held.clear(); for (PlacedEntry e : placed.values()) try { e.playback.stop(); } catch (Throwable t) {} placed.clear(); }
    public void stopChunk(ServerLevel level, net.minecraft.world.level.ChunkPos cp, String reason) { Iterator<Map.Entry<GlobalPos, PlacedEntry>> it = placed.entrySet().iterator(); while (it.hasNext()) { Map.Entry<GlobalPos, PlacedEntry> en = it.next(); GlobalPos gp = en.getKey(); if (gp.dimension().equals(level.dimension()) && cp.equals(new net.minecraft.world.level.ChunkPos(gp.pos()))) { try { en.getValue().playback.stop(); } catch (Throwable t) {} it.remove(); } } }
    private boolean canStartMore() { return (held.size() + placed.size()) < BoomboxConfig.BOOMBOX_MAX_CONCURRENT_SOURCES.get(); }
    public void tickHud(ServerPlayer player) { HeldEntry e = held.get(player.getUUID()); if (e == null || e.playback.isStopped()) { BoomboxNetworking.sendTimerState(player, false, 0L, 0L); return; } BoomboxNetworking.sendTimerState(player, true, e.playback.positionMs(), e.playback.durationMs()); }
    private static final class BoomboxHeldState { final ItemStack boombox; final CustomDiscData disc; BoomboxHeldState(ItemStack b, CustomDiscData d) { boombox = b; disc = d; } }
    private BoomboxHeldState readHeldBoombox(ServerPlayer player) {
        ItemStack main = player.getMainHandItem(), off = player.getOffhandItem(), boombox = null;
        if (main.getItem() == com.eyecrasher.lazoboombox.block.ModBlocks.BOOMBOX_ITEM.get()) boombox = main; else if (off.getItem() == com.eyecrasher.lazoboombox.block.ModBlocks.BOOMBOX_ITEM.get()) boombox = off;
        if (boombox == null || !BoomboxData.hasDisc(boombox)) return null;
        ItemStack disc = BoomboxData.readDisc(boombox, player.registryAccess()); if (disc.isEmpty()) return null;
        CustomDiscData data = DiscDataUtil.read(disc).orElse(null); return data == null ? null : new BoomboxHeldState(boombox, data);
    }
}
