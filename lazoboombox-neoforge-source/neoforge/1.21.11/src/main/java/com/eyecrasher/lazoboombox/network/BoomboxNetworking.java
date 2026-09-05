package com.eyecrasher.lazoboombox.network;
import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class BoomboxNetworking {
    public record BoomboxStatePayload(boolean active, long positionMs, long durationMs) implements CustomPacketPayload {
        public static final Identifier ID = Identifier.fromNamespaceAndPath(LazoBoombox.MOD_ID, "timer_state");
        public static final CustomPacketPayload.Type<BoomboxStatePayload> TYPE = new CustomPacketPayload.Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, BoomboxStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBoolean(p.active); buf.writeLong(p.positionMs); buf.writeLong(p.durationMs); },
            buf -> new BoomboxStatePayload(buf.readBoolean(), buf.readLong(), buf.readLong()));
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private BoomboxNetworking() {}
    public static void registerServer(IEventBus modBus) { modBus.addListener(BoomboxNetworking::onRegisterPayloads); }
    public static void registerClient(IEventBus modBus) { modBus.addListener(BoomboxNetworking::onRegisterPayloads); }
    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(LazoBoombox.MOD_ID).playToClient(BoomboxStatePayload.TYPE, BoomboxStatePayload.STREAM_CODEC,
            (payload, context) -> BoomboxHudOverlay.setState(payload.active(), payload.positionMs(), payload.durationMs()));
    }
    public static void sendTimerState(ServerPlayer player, boolean active, long positionMs, long durationMs) {
        try { player.connection.send(new BoomboxStatePayload(active, positionMs, durationMs)); } catch (Throwable ignored) {}
    }
}
