package com.eyecrasher.lazoboombox.network;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class BoomboxNetworking {
    private static final SimpleChannel CHANNEL =
            ChannelBuilder.named(
                            ResourceLocation.fromNamespaceAndPath(
                                    LazoBoombox.MOD_ID, "timer_state"))
                    .networkProtocolVersion(1)
                    .optionalClient()
                    .optionalServer()
                    .simpleChannel();
    private static boolean registered;

    private record TimerState(boolean active, long positionMs, long durationMs) {}

    private BoomboxNetworking() {}

    public static synchronized void registerServer(IEventBus modBus) {
        if (registered) return;
        CHANNEL.messageBuilder(TimerState.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(
                        (state, buffer) -> {
                            buffer.writeBoolean(state.active());
                            buffer.writeLong(state.positionMs());
                            buffer.writeLong(state.durationMs());
                        })
                .decoder(BoomboxNetworking::decode)
                .consumerMainThread((state, context) -> applyClientState(state))
                .add();
        CHANNEL.build();
        registered = true;
    }

    public static void registerClient(IEventBus modBus) {
        registerServer(modBus);
    }

    private static TimerState decode(FriendlyByteBuf buffer) {
        return new TimerState(buffer.readBoolean(), buffer.readLong(), buffer.readLong());
    }

    // Invoked only by the clientbound handler after Forge switches to the game thread.
    private static void applyClientState(TimerState state) {
        BoomboxHudOverlay.setState(state.active(), state.positionMs(), state.durationMs());
    }

    public static void sendTimerState(
            ServerPlayer player, boolean active, long positionMs, long durationMs) {
        if (CHANNEL.isRemotePresent(player.connection.getConnection())) {
            CHANNEL.send(
                    new TimerState(active, positionMs, durationMs),
                    PacketDistributor.PLAYER.with(player));
        }
    }
}
