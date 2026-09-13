package com.eyecrasher.lazoboombox;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import com.eyecrasher.lazoboombox.config.BoomboxConfig;
import com.eyecrasher.lazoboombox.event.BoomboxEvents;
import com.eyecrasher.lazoboombox.event.BoomboxSableEvents;
import com.eyecrasher.lazoboombox.item.ModCreativeTabs;
import com.eyecrasher.lazoboombox.item.ModItems;
import com.eyecrasher.lazoboombox.network.BoomboxNetworking;
import com.eyecrasher.lazoboombox.voice.LazoBoomboxServerBootstrap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
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

        // Register the Sable physics-tick listener (no-op if Sable isn't installed).
        // Done in FMLCommonSetupEvent so we don't touch Sable classes during early mod loading.
        modBus.addListener(LazoBoombox::onCommonSetup);
        LOGGER.info("LazoBoombox initialized (addon for LazoDiscs).");
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            BoomboxSableEvents.registerIfSablePresent();

            if (BoomboxConfig.ALLOW_PLAYBACK_ON_SABLE_PLATFORMS.get()) {
                LOGGER.warn(
                        "LazoBoombox: allowPlaybackOnSablePlatforms is enabled — this is an " +
                        "EXPERIMENTAL integration with Sable moving platforms and has not been " +
                        "battle-tested on a live server. Report any issues you hit.");
            }
        });
    }

    public static net.minecraft.server.MinecraftServer getCurrentServer() { return currentServer; }
    public static void setCurrentServer(net.minecraft.server.MinecraftServer server) { currentServer = server; }
}
