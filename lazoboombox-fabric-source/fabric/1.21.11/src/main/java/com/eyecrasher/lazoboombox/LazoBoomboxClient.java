package com.eyecrasher.lazoboombox;
import com.eyecrasher.lazoboombox.client.BoomboxClientConfig; import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;
import com.eyecrasher.lazoboombox.network.BoomboxNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
public final class LazoBoomboxClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        BoomboxClientConfig.load(); BoomboxNetworking.registerClient();
        HudRenderCallback.EVENT.register(new BoomboxHudOverlay());
    }
}
