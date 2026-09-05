package com.eyecrasher.lazoboombox.voice;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazodiscs.LazoDiscsServerBootstrap; import com.eyecrasher.lazodiscs.data.CustomDiscData;
import com.eyecrasher.lazodiscs.voice.LavaPcmFeeder; import net.minecraft.server.level.ServerLevel; import net.minecraft.server.level.ServerPlayer;
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
        return startInternal(server, line, disc, onFinished, () -> line.createEntitySource(mcEntity, false), true);
    }
    public static BoomboxPlayback start(ServerLevel level, Vec3 pos, CustomDiscData disc, Runnable onFinished) {
        PlasmoVoiceServer server = voiceServer; ServerSourceLine line = boomboxLine;
        if (server == null || line == null) throw new IllegalStateException("LazoBoombox PV source line not initialized");
        Optional<McServerWorld> wr = findWorld(server, level); if (wr.isEmpty()) throw new IllegalStateException("Could not resolve PV world for " + dimensionId(level));
        ServerPos3d pvPos = new ServerPos3d(wr.get(), pos.x, pos.y, pos.z);
        return startInternal(server, line, disc, onFinished, () -> line.createStaticSource(pvPos, false), false);
    }
    private static BoomboxPlayback startInternal(PlasmoVoiceServer server, ServerSourceLine line, CustomDiscData disc, Runnable onFinished, java.util.function.Supplier<ServerProximitySource<?>> sc, boolean isEntity) {
        AtomicBoolean stopped = new AtomicBoolean(false), manualStop = new AtomicBoolean(false), finishedNotified = new AtomicBoolean(false);
        AtomicReference<LavaPcmFeeder.StreamingPlayback> playbackRef = new AtomicReference<>();
        AtomicReference<ServerProximitySource<?>> sourceRef = new AtomicReference<>(); AtomicReference<AudioSender> senderRef = new AtomicReference<>(); AtomicReference<Future<?>> taskRef = new AtomicReference<>();
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
                LazoBoombox.LOGGER.info("Boombox audio started for '{}' (entity={})", disc.title(), isEntity);
            } catch (Throwable t) { if (!stopped.get()) LazoBoombox.LOGGER.warn("Failed to start Boombox audio: {}", t.toString()); stopped.set(true); cleanup.run(); if (!manualStop.get()) notifyFinished.run(); }
        });
        taskRef.set(task);
        Runnable stopAction = () -> { if (!stopped.compareAndSet(false, true)) return; manualStop.set(true); var t = taskRef.getAndSet(null); if (t != null) t.cancel(true); var s = senderRef.getAndSet(null); if (s != null) try { s.stop(); } catch (Exception e) {} cleanup.run(); };
        BiConsumer<ServerLevel, Vec3> pu = (ul, projected) -> { if (isEntity) return; if (stopped.get()) return; var s = sourceRef.get(); if (s == null) return; if (!(s instanceof ServerStaticSource)) return;
            try { var w = findWorld(server, ul).orElseThrow(); ((ServerStaticSource) s).setPosition(new ServerPos3d(w, projected.x, projected.y, projected.z)); } catch (Exception e) {} };
        return new BoomboxPlayback(disc.title(), playbackRef, stopped, stopAction, pu);
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
