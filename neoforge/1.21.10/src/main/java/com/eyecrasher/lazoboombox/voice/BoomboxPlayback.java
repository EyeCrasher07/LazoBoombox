package com.eyecrasher.lazoboombox.voice;
import net.minecraft.server.level.ServerLevel; import net.minecraft.world.phys.Vec3;
import java.util.concurrent.atomic.AtomicReference; import java.util.function.BiConsumer;
public final class BoomboxPlayback {
    private final String title; private final AtomicReference<com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback> playback;
    private final java.util.concurrent.atomic.AtomicBoolean stopped; private final Runnable stopAction; private final BiConsumer<ServerLevel, Vec3> positionUpdater;
    private final java.util.concurrent.atomic.AtomicBoolean dynamicPosition;
    BoomboxPlayback(String title, AtomicReference<com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback> p, java.util.concurrent.atomic.AtomicBoolean s, Runnable sa, BiConsumer<ServerLevel, Vec3> pu, boolean dynamicPosition) { this.title = title; this.playback = p; this.stopped = s; this.stopAction = sa; this.positionUpdater = pu; this.dynamicPosition = new java.util.concurrent.atomic.AtomicBoolean(dynamicPosition); }
    public void stop() { stopAction.run(); }
    public void updatePosition(ServerLevel level, Vec3 pos) { positionUpdater.accept(level, pos); }
    public boolean isStopped() { return stopped.get(); }
    public String title() { return title; }
    /** Returns true iff this playback is on a Sable moving platform and needs per-tick position updates. */
    public boolean isDynamicPosition() { return dynamicPosition.get(); }
    /**
     * Switches this playback into dynamic-position mode after it already started (e.g. the
     * placed boombox was a perfectly normal block when playback started, and only later got
     * swept into a Sable sub-level via the Physics Assembler). Idempotent.
     */
    public void markDynamicPosition() { dynamicPosition.set(true); }
    public long positionMs() { var p = playback.get(); if (p == null) return 0L; try { return p.track().getPosition(); } catch (Exception e) { return 0L; } }
    public long durationMs() { var p = playback.get(); if (p == null) return 0L; try { return p.track().getDuration(); } catch (Exception e) { return 0L; } }
}
