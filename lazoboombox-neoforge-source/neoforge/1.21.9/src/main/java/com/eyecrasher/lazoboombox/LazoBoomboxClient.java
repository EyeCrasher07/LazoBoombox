package com.eyecrasher.lazoboombox;
import com.eyecrasher.lazoboombox.client.BoomboxClientConfig;
import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = LazoBoombox.MOD_ID, dist = Dist.CLIENT)
public final class LazoBoomboxClient {
    public LazoBoomboxClient(IEventBus modBus) {
        BoomboxClientConfig.load();
        NeoForge.EVENT_BUS.addListener(BoomboxHudOverlay::onRender);
    }
}
