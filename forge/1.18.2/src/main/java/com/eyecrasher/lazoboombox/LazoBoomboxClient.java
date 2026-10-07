package com.eyecrasher.lazoboombox;

import com.eyecrasher.lazoboombox.client.BoomboxClientConfig;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

public final class LazoBoomboxClient {
    private LazoBoomboxClient() {}

    public static void initialize() {
        BoomboxClientConfig.load();
        MinecraftForge.EVENT_BUS.addListener(LazoBoomboxClient::onRenderHud);
        MinecraftForge.EVENT_BUS.addListener(
                (net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event) ->
                        BoomboxHudOverlay.setState(false, 0L, 0L));
    }

    // Forge overrides vanilla HUD rendering on this version.
    private static void onRenderHud(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            BoomboxHudOverlay.render(event.getMatrixStack(), event.getPartialTicks());
        }
    }
}
