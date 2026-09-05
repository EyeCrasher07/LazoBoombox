package com.eyecrasher.lazoboombox;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.event.BoomboxEvents;
import com.eyecrasher.lazoboombox.item.ModCreativeTabs;
import com.eyecrasher.lazoboombox.item.ModItems;
import com.eyecrasher.lazoboombox.network.BoomboxNetworking;
import com.eyecrasher.lazoboombox.voice.LazoBoomboxServerBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LazoBoombox.MOD_ID)
public final class LazoBoombox {
    public static final String MOD_ID = "lazoboombox";
    public static final Logger LOGGER = LoggerFactory.getLogger("LazoBoombox");
    private static volatile net.minecraft.server.MinecraftServer currentServer;

    public LazoBoombox(IEventBus modBus) {
        BoomboxConfig.load();
        BoomboxNetworking.registerServer(modBus);
        ModBlocks.register(modBus);
        ModItems.initialize();
        ModCreativeTabs.initialize(modBus);
        NeoForge.EVENT_BUS.register(BoomboxEvents.class);
        LazoBoomboxServerBootstrap.loadPlasmoAddon();
        LOGGER.info("LazoBoombox initialized (addon for LazoDiscs).");
    }

    public static net.minecraft.server.MinecraftServer getCurrentServer() { return currentServer; }
    public static void setCurrentServer(net.minecraft.server.MinecraftServer server) { currentServer = server; }
}
