package com.eyecrasher.lazoboombox.voice;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import su.plo.voice.api.addon.AddonInitializer; import su.plo.voice.api.addon.InjectPlasmoVoice; import su.plo.voice.api.addon.annotation.Addon; import su.plo.voice.api.server.PlasmoVoiceServer;
import java.io.InputStream;
@Addon(id = "lazoboombox", name = "LazoBoombox", version = "0.1.0+mc1.21.11", authors = {"EyeCrasher"})
public final class LazoBoomboxVoiceAddon implements AddonInitializer {
    @InjectPlasmoVoice private PlasmoVoiceServer voiceServer;
    @Override public void onAddonInitialize() {
        try { var manager = voiceServer.getSourceLineManager();
            InputStream icon = getClass().getClassLoader().getResourceAsStream("assets/lazoboombox/icon.png");
            su.plo.voice.api.server.audio.line.ServerSourceLine line;
            if (icon != null) { var b = manager.createBuilder(this, "boombox", "Boombox", icon, 50); b.setDefaultVolume(1.0F); line = b.build(); }
            else { var b = manager.createBuilder(this, "boombox", "Boombox", "lazoboombox:textures/icons/boombox.png", 50); b.setDefaultVolume(1.0F); line = b.build(); }
            BoomboxAudioEngine.initialize(voiceServer, line);
            LazoBoombox.LOGGER.info("LazoBoombox PV source line 'boombox' registered");
        } catch (Exception e) { throw new IllegalStateException("Could not register LazoBoombox PV source line", e); }
    }
    @Override public void onAddonShutdown() { BoomboxPlaybackManager.INSTANCE.stopAll("pv-addon-shutdown"); BoomboxAudioEngine.shutdown(); }
}
