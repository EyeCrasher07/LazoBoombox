package com.eyecrasher.lazoboombox.network;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class BoomboxNetworking {
    private static final ResourceLocation TIMER_STATE =
            new ResourceLocation(LazoBoombox.MOD_ID, "timer_state");

    private BoomboxNetworking() {}

    public static void registerServer() {
        // This version registers clientbound channels through the client receiver.
    }

    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                TIMER_STATE,
                (client, handler, buffer, responseSender) -> {
                    boolean active = buffer.readBoolean();
                    long positionMs = buffer.readLong();
                    long durationMs = buffer.readLong();
                    client.execute(
                            () -> BoomboxHudOverlay.setState(active, positionMs, durationMs));
                });
    }

    public static void sendTimerState(
            ServerPlayer player, boolean active, long positionMs, long durationMs) {
        if (!ServerPlayNetworking.canSend(player, TIMER_STATE)) return;
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeBoolean(active);
        buffer.writeLong(positionMs);
        buffer.writeLong(durationMs);
        ServerPlayNetworking.send(player, TIMER_STATE, buffer);
    }
}
