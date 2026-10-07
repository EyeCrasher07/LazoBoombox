package com.eyecrasher.lazoboombox.network;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class BoomboxNetworking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(LazoBoombox.MOD_ID, "timer_state"),
                    () -> PROTOCOL,
                    NetworkRegistry.acceptMissingOr(PROTOCOL),
                    NetworkRegistry.acceptMissingOr(PROTOCOL));
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
                .consumer(
                        (state, context) -> {
                            context.get().enqueueWork(() -> applyClientState(state));
                            context.get().setPacketHandled(true);
                        })
                .add();
        registered = true;
    }

    public static void registerClient(IEventBus modBus) {
        registerServer(modBus);
    }

    private static TimerState decode(PacketBuffer buffer) {
        return new TimerState(buffer.readBoolean(), buffer.readLong(), buffer.readLong());
    }

    // Invoked only by the clientbound handler after Forge switches to the game thread.
    private static void applyClientState(TimerState state) {
        BoomboxHudOverlay.setState(state.active(), state.positionMs(), state.durationMs());
    }

    public static void sendTimerState(
            ServerPlayerEntity player, boolean active, long positionMs, long durationMs) {
        if (CHANNEL.isRemotePresent(player.connection.connection)) {
            CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new TimerState(active, positionMs, durationMs));
        }
    }
}
