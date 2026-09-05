package com.eyecrasher.lazoboombox.voice;
import com.eyecrasher.lazoboombox.LazoBoombox; import su.plo.voice.api.server.PlasmoVoiceServer;
public final class LazoBoomboxServerBootstrap {
    private static volatile boolean loaded = false;
    private LazoBoomboxServerBootstrap() {}
    public static synchronized void loadPlasmoAddon() {
        if (loaded) return;
        try { PlasmoVoiceServer.getAddonsLoader().load(new LazoBoomboxVoiceAddon()); loaded = true; LazoBoombox.LOGGER.info("LazoBoombox Plasmo Voice addon loaded"); }
        catch (Throwable t) { LazoBoombox.LOGGER.warn("Could not load LazoBoombox PV addon: {}", t.toString()); }
    }
    public static boolean isLoaded() { return loaded; }
    public static synchronized void resetForIntegratedServer() { loaded = false; }
}
