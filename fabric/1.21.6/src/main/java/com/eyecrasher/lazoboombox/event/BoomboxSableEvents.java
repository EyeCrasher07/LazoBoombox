package com.eyecrasher.lazoboombox.event;

import com.eyecrasher.lazoboombox.LazoBoombox;
import com.eyecrasher.lazoboombox.compat.BoomboxSableCompat;
import com.eyecrasher.lazoboombox.server.BoomboxPlaybackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/**
 * Listens to Sable's post-physics-tick event to proactively update Plasmo Voice source
 * positions for placed boomboxes on moving platforms.
 *
 * <p>Mirror of LazoDiscs's {@code SablePhysicsEvents}. This class is <b>loader-agnostic</b>
 * and uses <b>pure reflection</b> to access Sable's {@code SableEventPlatform} cross-loader
 * SPI. No compile-time dependency on Sable is needed.
 *
 * <h3>Why LazoBoombox needs this</h3>
 * <p>Without this listener, placed boomboxes on Sable moving platforms would only get their
 * position updated every 20 ticks (1 second) via
 * {@link com.eyecrasher.lazoboombox.block.BoomboxBlockEntity#serverTick}. For fast-moving
 * airships this would cause audible lag. By subscribing to Sable's post-physics-tick
 * (which fires after every Sable physics step, typically at 60 Hz), we reduce the lag
 * to ~16 ms.
 */
public final class BoomboxSableEvents {

    private BoomboxSableEvents() {
    }

    private static volatile boolean registered = false;

    // Reflection-cached SableEventPlatform.
    private static volatile boolean platformProbed = false;
    private static volatile Object platformInstance;
    private static volatile Method onPostPhysicsTickMethod;

    /**
     * Registers the Sable physics-tick listener via the cross-loader
     * {@code SableEventPlatform} SPI, but ONLY if Sable is actually installed.
     * Idempotent.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerIfSablePresent() {
        if (registered) return;
        synchronized (BoomboxSableEvents.class) {
            if (registered) return;

            if (!BoomboxSableCompat.isSableLoaded()) {
                LazoBoombox.LOGGER.info(
                        "Sable not detected; skipping SablePostPhysicsTickEvent registration. " +
                        "LazoBoombox will fall back to per-20-tick position updates only."
                );
                return;
            }

            try {
                ensurePlatformProbed();
                Object platform = platformInstance;
                Method onPostPhysicsTick = onPostPhysicsTickMethod;
                if (platform == null || onPostPhysicsTick == null) {
                    LazoBoombox.LOGGER.warn(
                            "Sable is loaded but SableEventPlatform.INSTANCE is null or " +
                            "onPostPhysicsTick method not found. " +
                            "Falling back to per-20-tick position updates only."
                    );
                    return;
                }

                BiConsumer<Object, Object> callback = (physicsSystem, timeStep) -> {
                    try {
                        MinecraftServer server = LazoBoombox.getCurrentServer();
                        if (server == null) return;

                        for (ServerLevel level : server.getAllLevels()) {
                            BoomboxPlaybackManager.INSTANCE.onSablePostPhysicsTick(level);
                        }
                    } catch (Throwable t) {
                        LazoBoombox.LOGGER.debug(
                                "Error during SablePostPhysicsTickEvent handling for boomboxes: {}",
                                t.toString()
                        );
                    }
                };

                onPostPhysicsTick.invoke(platform, (Object) callback);

                registered = true;
                LazoBoombox.LOGGER.info(
                        "Sable detected; registered SablePostPhysicsTickEvent listener via " +
                        "SableEventPlatform for low-latency Plasmo Voice position updates " +
                        "on moving boomboxes."
                );
            } catch (Throwable t) {
                LazoBoombox.LOGGER.warn(
                        "Failed to register SablePostPhysicsTickEvent listener: {}",
                        t.toString()
                );
            }
        }
    }

    /**
     * Probes for the SableEventPlatform class and its INSTANCE field + onPostPhysicsTick method
     * via reflection. Idempotent.
     */
    private static void ensurePlatformProbed() {
        if (platformProbed) return;
        synchronized (BoomboxSableEvents.class) {
            if (platformProbed) return;
            try {
                Class<?> platformClass = Class.forName(
                        "dev.ryanhcode.sable.platform.SableEventPlatform");
                Field instanceField = platformClass.getField("INSTANCE");
                Object instance = instanceField.get(null);
                if (instance != null) {
                    for (Method m : platformClass.getMethods()) {
                        if ("onPostPhysicsTick".equals(m.getName())
                                && m.getParameterCount() == 1
                                && m.getParameterTypes()[0] == BiConsumer.class) {
                            platformInstance = instance;
                            onPostPhysicsTickMethod = m;
                            break;
                        }
                    }
                    if (onPostPhysicsTickMethod == null) {
                        LazoBoombox.LOGGER.warn(
                                "SableEventPlatform found but onPostPhysicsTick(BiConsumer) " +
                                "method not found. Sable physics-tick integration disabled."
                        );
                    }
                }
            } catch (Throwable t) {
                LazoBoombox.LOGGER.debug(
                        "SableEventPlatform probe failed: {}", t.toString());
            } finally {
                platformProbed = true;
            }
        }
    }
}
