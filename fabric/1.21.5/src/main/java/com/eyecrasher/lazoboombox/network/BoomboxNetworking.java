package com.eyecrasher.lazoboombox.network;
import com.eyecrasher.lazoboombox.LazoBoombox; import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking; import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry; import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf; import net.minecraft.network.codec.StreamCodec; import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation; import net.minecraft.server.level.ServerPlayer;
public final class BoomboxNetworking {
    public record BoomboxStatePayload(boolean active, long positionMs, long durationMs) implements CustomPacketPayload {
        public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(LazoBoombox.MOD_ID, "timer_state");
        public static final CustomPacketPayload.Type<BoomboxStatePayload> TYPE = new CustomPacketPayload.Type<>(ID);
        public static final StreamCodec<FriendlyByteBuf, BoomboxStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeBoolean(p.active); buf.writeLong(p.positionMs); buf.writeLong(p.durationMs); },
            buf -> new BoomboxStatePayload(buf.readBoolean(), buf.readLong(), buf.readLong()));
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private BoomboxNetworking() {}
    public static void registerServer() { PayloadTypeRegistry.playS2C().register(BoomboxStatePayload.TYPE, BoomboxStatePayload.STREAM_CODEC); }
    public static void registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(BoomboxStatePayload.TYPE, (payload, context) ->
            BoomboxHudOverlay.setState(payload.active(), payload.positionMs(), payload.durationMs()));
    }
    public static void sendTimerState(ServerPlayer player, boolean active, long positionMs, long durationMs) {
        try { ServerPlayNetworking.send(player, new BoomboxStatePayload(active, positionMs, durationMs)); } catch (Throwable ignored) {}
    }
}
