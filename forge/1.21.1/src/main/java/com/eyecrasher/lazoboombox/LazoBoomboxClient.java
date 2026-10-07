package com.eyecrasher.lazoboombox;

import com.eyecrasher.lazoboombox.client.BoomboxClientConfig;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;

import net.minecraftforge.common.MinecraftForge;

public final class LazoBoomboxClient {
    private LazoBoomboxClient() {}

    public static void initialize() {
        BoomboxClientConfig.load();
        MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) ->
                        BoomboxHudOverlay.setState(false, 0L, 0L));
    }
}
