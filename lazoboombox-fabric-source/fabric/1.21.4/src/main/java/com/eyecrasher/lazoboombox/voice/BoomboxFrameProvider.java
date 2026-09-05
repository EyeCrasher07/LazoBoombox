package com.eyecrasher.lazoboombox.voice;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState; import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import su.plo.voice.api.server.PlasmoVoiceServer; import su.plo.voice.api.server.audio.provider.AudioFrameProvider; import su.plo.voice.api.server.audio.provider.AudioFrameResult;
import java.util.concurrent.atomic.AtomicBoolean;
final class BoomboxFrameProvider implements AudioFrameProvider {
    private final PlasmoVoiceServer server; private final com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback playback; private final AtomicBoolean stopped;
    BoomboxFrameProvider(PlasmoVoiceServer s, com.eyecrasher.lazodiscs.voice.LavaPcmFeeder.StreamingPlayback p, AtomicBoolean st) { server = s; playback = p; stopped = st; }
    @Override public AudioFrameResult provide20ms() {
        if (stopped.get()) return AudioFrameResult.Finished.INSTANCE;
        try { AudioTrackState state = playback.track().getState();
            if (state == AudioTrackState.FINISHED || (state == AudioTrackState.INACTIVE && playback.track().getPosition() > 0L)) return AudioFrameResult.Finished.INSTANCE;
            AudioFrame frame = playback.player().provide(); byte[] data = frame == null ? null : frame.getData();
            byte[] encrypted = data == null ? null : server.getDefaultEncryption().encrypt(data);
            return new AudioFrameResult.Provided(encrypted);
        } catch (Throwable t) { stopped.set(true); return AudioFrameResult.Finished.INSTANCE; }
    }
}
