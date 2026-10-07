package com.eyecrasher.lazoboombox;

import com.eyecrasher.lazoboombox.client.BoomboxClientConfig;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;
import com.eyecrasher.lazoboombox.network.BoomboxNetworking;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.resources.Identifier;

public final class LazoBoomboxClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BoomboxClientConfig.load();
        com.eyecrasher.lazoboombox.client.BoomboxColors.initialize();
        BoomboxNetworking.registerClient();
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> BoomboxHudOverlay.setState(false, 0L, 0L));
        // Inherit the vanilla hotbar's visibility condition, including the hide-HUD key.
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.fromNamespaceAndPath(LazoBoombox.MOD_ID, "timer"),
                new BoomboxHudOverlay());
    }
}
