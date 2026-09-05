package com.eyecrasher.lazoboombox.voice;
import net.minecraft.server.level.ServerLevel; import net.minecraft.world.phys.Vec3;
import java.util.concurrent.atomic.AtomicReference; import java.util.function.BiConsumer;
public final class BoomboxPlayback {
    private final String title; private final AtomicReference<com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback> playback;
    private final java.util.concurrent.atomic.AtomicBoolean stopped; private final Runnable stopAction; private final BiConsumer<ServerLevel, Vec3> positionUpdater;
    BoomboxPlayback(String title, AtomicReference<com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback> p, java.util.concurrent.atomic.AtomicBoolean s, Runnable sa, BiConsumer<ServerLevel, Vec3> pu) { this.title = title; this.playback = p; this.stopped = s; this.stopAction = sa; this.positionUpdater = pu; }
    public void stop() { stopAction.run(); }
    public void updatePosition(ServerLevel level, Vec3 pos) { positionUpdater.accept(level, pos); }
    public boolean isStopped() { return stopped.get(); }
    public String title() { return title; }
    public long positionMs() { var p = playback.get(); if (p == null) return 0L; try { return p.track().getPosition(); } catch (Exception e) { return 0L; } }
    public long durationMs() { var p = playback.get(); if (p == null) return 0L; try { return p.track().getDuration(); } catch (Exception e) { return 0L; } }
}
