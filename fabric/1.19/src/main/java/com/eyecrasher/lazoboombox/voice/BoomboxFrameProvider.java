package com.eyecrasher.lazoboombox.voice;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import su.plo.voice.api.server.PlasmoVoiceServer;
import su.plo.voice.api.server.audio.provider.AudioFrameProvider;
import su.plo.voice.api.server.audio.provider.AudioFrameResult;

import java.util.concurrent.atomic.AtomicBoolean;

final class BoomboxFrameProvider implements AudioFrameProvider {
    private final PlasmoVoiceServer server;
    private final com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback playback;
    private final AtomicBoolean stopped;
    private final java.util.concurrent.atomic.AtomicReference<Throwable> failure;

    BoomboxFrameProvider(
            PlasmoVoiceServer s,
            com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback p,
            AtomicBoolean st,
            java.util.concurrent.atomic.AtomicReference<Throwable> failure) {
        server = s;
        playback = p;
        stopped = st;
        this.failure = failure;
    }

    @Override
    public AudioFrameResult provide20ms() {
        if (stopped.get()) return AudioFrameResult.Finished.INSTANCE;
        try {
            AudioFrame frame = playback.player().provide();
            if (frame != null)
                return new AudioFrameResult.Provided(
                        server.getDefaultEncryption().encrypt(frame.getData()));
            AudioTrackState state = playback.track().getState();
            if (state == AudioTrackState.FINISHED
                    || (state == AudioTrackState.INACTIVE && playback.track().getPosition() > 0L))
                return AudioFrameResult.Finished.INSTANCE;
            return new AudioFrameResult.Provided(null);
        } catch (Throwable t) {
            failure.compareAndSet(null, t);
            stopped.set(true);
            return AudioFrameResult.Finished.INSTANCE;
        }
    }
}
