package com.eyecrasher.lazodiscs.voice;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer; import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
public final class LavaPcmFeeder {
    private LavaPcmFeeder() {}
    public static StreamingPlayback openStream(String rawUrl, String title, float volume) throws InterruptedException {
        throw new UnsupportedOperationException("LazoPcmFeeder stub — real LazoDiscs must be installed at runtime");
    }
    public record StreamingPlayback(AudioPlayer player, AudioTrack track) implements AutoCloseable {
        @Override public void close() { try { player.stopTrack(); } catch (Exception e) {} try { player.destroy(); } catch (Exception e) {} }
    }
}
