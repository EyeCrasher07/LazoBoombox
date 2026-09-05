package com.eyecrasher.lazoboombox;
import com.eyecrasher.lazoboombox.block.ModBlocks; import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.event.BoomboxEvents; import com.eyecrasher.lazoboombox.item.ModCreativeTabs;
import com.eyecrasher.lazoboombox.item.ModItems; import com.eyecrasher.lazoboombox.network.BoomboxNetworking;
import net.fabricmc.api.ModInitializer; import org.slf4j.Logger; import org.slf4j.LoggerFactory;
public final class LazoBoombox implements ModInitializer {
    public static final String MOD_ID = "lazoboombox";
    public static final Logger LOGGER = LoggerFactory.getLogger("LazoBoombox");
    private static volatile net.minecraft.server.MinecraftServer currentServer;
    @Override public void onInitialize() {
        BoomboxConfig.load(); BoomboxNetworking.registerServer();
        ModBlocks.initialize(); ModItems.initialize(); ModCreativeTabs.initialize(); BoomboxEvents.register();
        LOGGER.info("LazoBoombox initialized (addon for LazoDiscs).");
    }
    public static net.minecraft.server.MinecraftServer getCurrentServer() { return currentServer; }
    public static void setCurrentServer(net.minecraft.server.MinecraftServer server) { currentServer = server; }
}
