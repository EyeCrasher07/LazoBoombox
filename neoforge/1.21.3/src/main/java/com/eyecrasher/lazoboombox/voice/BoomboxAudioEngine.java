package com.eyecrasher.lazoboombox.voice;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.compat.BoomboxSableCompat; import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazodiscs.LazoDiscsServerBootstrap; import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.voice.LavaPcmFeeder; import net.minecraft.core.BlockPos; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3; import su.plo.slib.api.server.entity.McServerEntity; import su.plo.slib.api.server.position.ServerPos3d; import su.plo.slib.api.server.world.McServerWorld;
import su.plo.voice.api.server.PlasmoVoiceServer; import su.plo.voice.api.server.audio.line.ServerSourceLine; import su.plo.voice.api.server.audio.source.AudioSender;
import su.plo.voice.api.server.audio.source.ServerProximitySource; import su.plo.voice.api.server.audio.source.ServerEntitySource; import su.plo.voice.api.server.audio.source.ServerStaticSource;
import java.util.Locale; import java.util.Optional; import java.util.concurrent.ExecutorService; import java.util.concurrent.Executors;
import java.util.concurrent.Future; import java.util.concurrent.atomic.AtomicBoolean; import java.util.concurrent.atomic.AtomicReference; import java.util.function.BiConsumer;
public final class BoomboxAudioEngine {
    private static final ExecutorService LOAD_EXECUTOR = Executors.newFixedThreadPool(4, r -> { Thread t = new Thread(r, "lazoboombox-audio-loader"); t.setDaemon(true); return t; });
    private static volatile PlasmoVoiceServer voiceServer; private static volatile ServerSourceLine boomboxLine;
    private BoomboxAudioEngine() {}
    static void initialize(PlasmoVoiceServer server, ServerSourceLine line) { voiceServer = server; boomboxLine = line; }
    static void shutdown() { voiceServer = null; boomboxLine = null; }
    public static boolean isReady() { return voiceServer != null && boomboxLine != null && LazoDiscsServerBootstrap.isLoaded(); }
    public static BoomboxPlayback startEntity(ServerPlayer player, CustomDiscData disc, Runnable onFinished) {
        PlasmoVoiceServer server = voiceServer; ServerSourceLine line = boomboxLine;
        if (server == null || line == null) throw new IllegalStateException("LazoBoombox PV source line not initialized");
        McServerEntity mcEntity;
        try { var vp = server.getPlayerManager().getPlayerByInstance(player); if (vp == null) throw new IllegalStateException("PV player not found"); mcEntity = vp.getInstance(); }
        catch (Throwable t) { throw new IllegalStateException("Could not get McServerEntity", t); }
        return startInternal(server, line, disc, onFinished, () -> line.createEntitySource(mcEntity, false), true, false);
    }
    public static BoomboxPlayback start(ServerLevel level, Vec3 pos, CustomDiscData disc, Runnable onFinished) {
        PlasmoVoiceServer server = voiceServer; ServerSourceLine line = boomboxLine;
        if (server == null || line == null) throw new IllegalStateException("LazoBoombox PV source line not initialized");
        Optional<McServerWorld> wr = findWorld(server, level); if (wr.isEmpty()) throw new IllegalStateException("Could not resolve PV world for " + dimensionId(level));

        // Project the position from sub-level coordinates to real-world coordinates.
        // IMPORTANT: use projectBoomboxCenter (not project) — project() has an early return
        // for |X|,|Z| < 1_000_000 that bypasses the Sable API call entirely, so it silently
        // produces a wrong result when Sable uses small plot offsets.  projectBoomboxCenter
        // always calls Sable.HELPER.projectOutOfSubLevel(), matching what LazoDiscs does with
        // SablePositionCompat.projectJukeboxCenter().
        Vec3 projectedPos = BoomboxSableCompat.projectBoomboxCenter(level, BlockPos.containing(pos));
        // OR in the coordinate heuristic (not just "did the position actually change"): if the
        // block genuinely is on a sub-level but projection fails/throws and falls back to the
        // raw position, projectedPos.equals(pos) would be true and we'd wrongly mark this
        // source as static forever. isProbablySubLevel doesn't depend on the projection call
        // succeeding, so it catches that case (mirrors LazoDiscs's JukeboxPlaybackManager).
        boolean dynamicPosition = BoomboxSableCompat.isProbablySubLevel(BlockPos.containing(pos))
                || !projectedPos.equals(pos);

        ServerPos3d pvPos = new ServerPos3d(wr.get(), projectedPos.x, projectedPos.y, projectedPos.z);
        return startInternal(server, line, disc, onFinished, () -> line.createStaticSource(pvPos, false), false, dynamicPosition);
    }
    private static BoomboxPlayback startInternal(PlasmoVoiceServer server, ServerSourceLine line, CustomDiscData disc, Runnable onFinished, java.util.function.Supplier<ServerProximitySource<?>> sc, boolean isEntity, boolean dynamicPosition) {
        AtomicBoolean stopped = new AtomicBoolean(false), manualStop = new AtomicBoolean(false), finishedNotified = new AtomicBoolean(false);
        AtomicReference<LavaPcmFeeder.StreamingPlayback> playbackRef = new AtomicReference<>();
        AtomicReference<ServerProximitySource<?>> sourceRef = new AtomicReference<>(); AtomicReference<AudioSender> senderRef = new AtomicReference<>(); AtomicReference<Future<?>> taskRef = new AtomicReference<>();
        // Optimization: remember the last projected position so we can skip redundant setPosition calls
        // when the platform hasn't actually moved (see updatePosition lambda below).
        AtomicReference<Vec3> lastProjectedPosition = new AtomicReference<>();
        Runnable cleanup = () -> { var p = playbackRef.getAndSet(null); if (p != null) try { p.close(); } catch (Exception e) {} var s = sourceRef.getAndSet(null); if (s != null) try { s.remove(); } catch (Exception e) {} };
        Runnable notifyFinished = () -> { if (onFinished == null) return; if (!finishedNotified.compareAndSet(false, true)) return; try { LazoBoombox.getCurrentServer().execute(onFinished); } catch (Exception e) {} };
        Future<?> task = LOAD_EXECUTOR.submit(() -> {
            try { if (stopped.get()) return;
                float vol = disc.volume() * (float) BoomboxConfig.BOOMBOX_VOLUME.get();
                var playback = LavaPcmFeeder.openStream(disc.url(), disc.title(), vol);
                if (stopped.get()) { playback.close(); return; } playbackRef.set(playback);
                ServerProximitySource<?> source = sc.get(); source.setName(disc.title()); sourceRef.set(source);
                var provider = new BoomboxFrameProvider(server, playback, stopped);
                int range = effectiveRange(disc.range()); AudioSender sender = source.createAudioSender(provider, (short) Math.max(1, Math.min(Short.MAX_VALUE, range))); senderRef.set(sender);
                sender.onStop(() -> { boolean wm = manualStop.get(); stopped.set(true); cleanup.run(); if (!wm) notifyFinished.run(); });
                if (stopped.get()) { cleanup.run(); return; } sender.start();
                LazoBoombox.LOGGER.info("Boombox audio started for '{}' (entity={}, dynamic={})", disc.title(), isEntity, dynamicPosition);
            } catch (Throwable t) { if (!stopped.get()) LazoBoombox.LOGGER.warn("Failed to start Boombox audio: {}", t.toString()); stopped.set(true); cleanup.run(); if (!manualStop.get()) notifyFinished.run(); }
        });
        taskRef.set(task);
        Runnable stopAction = () -> { if (!stopped.compareAndSet(false, true)) return; manualStop.set(true); var t = taskRef.getAndSet(null); if (t != null) t.cancel(true); var s = senderRef.getAndSet(null); if (s != null) try { s.stop(); } catch (Exception e) {} cleanup.run(); };
        // Position updater — only relevant for static (placed) sources. For entity sources,
        // Plasmo Voice automatically tracks the entity position; we no-op.
        //
        // Optimization: skip setPosition if the new position is within 0.01 blocks of the previous
        // one — stationary platforms (the common case) do not cause Plasmo client-side source-info
        // rebuilds.
        BiConsumer<ServerLevel, Vec3> pu = (ul, projected) -> {
            if (isEntity) return;
            if (stopped.get()) return;
            var s = sourceRef.get();
            if (s == null) return;
            if (!(s instanceof ServerStaticSource)) return;

            // Skip-if-unchanged optimization.
            Vec3 last = lastProjectedPosition.get();
            if (last != null && last.distanceToSqr(projected) < 1.0e-4D) return;
            lastProjectedPosition.set(projected);

            try {
                var w = findWorld(server, ul).orElseThrow();
                ((ServerStaticSource) s).setPosition(new ServerPos3d(w, projected.x, projected.y, projected.z));
            } catch (Exception e) {
                LazoBoombox.LOGGER.debug("Failed to update Boombox static source position: {}", e.toString());
            }
        };
        return new BoomboxPlayback(disc.title(), playbackRef, stopped, stopAction, pu, dynamicPosition);
    }
    static int effectiveRange(int discRange) {
        double vol = BoomboxConfig.BOOMBOX_VOLUME.get(); int max = BoomboxConfig.BOOMBOX_MAX_RADIUS.get();
        int base = Math.max(1, Math.min(max, discRange));
        if (BoomboxConfig.BOOMBOX_RADIUS_FOLLOWS_VOLUME.get()) return Math.max(0, (int) Math.round(base * Math.max(0.0, Math.min(1.0, vol))));
        return base;
    }
    private static Optional<McServerWorld> findWorld(PlasmoVoiceServer server, ServerLevel level) { String key = dimensionId(level); return server.getMinecraftServer().getWorlds().stream().filter(w -> w.getName().equalsIgnoreCase(key)).findFirst(); }
    private static String dimensionId(ServerLevel level) { return parseDimensionKey(level.dimension().toString()); }
    private static String parseDimensionKey(String s) { if (s == null || s.isEmpty()) return "unknown"; String r = s.trim(); if (r.startsWith("ResourceKey[") && r.contains(" / ") && r.endsWith("]")) { int i = r.lastIndexOf(" / "); if (i >= 0) return r.substring(i + 3, r.length() - 1).toLowerCase(Locale.ROOT); } return r.toLowerCase(Locale.ROOT); }
}
