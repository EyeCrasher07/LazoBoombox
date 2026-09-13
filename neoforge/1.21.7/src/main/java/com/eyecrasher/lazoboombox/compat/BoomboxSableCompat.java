package com.eyecrasher.lazoboombox.compat;

import com.eyecrasher.lazoboombox.LazoBoombox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional Sable compatibility for LazoBoombox.
 *
 * <p>Mirror of LazoDiscs's {@code SablePositionCompat}, using <b>pure reflection</b> so
 * LazoBoombox can load without Sable installed and without any compile-time dependency
 * on Sable.
 *
 * <h3>Why LazoBoombox needs this</h3>
 * <p>LazoBoombox's placed boombox creates a Plasmo Voice {@code ServerStaticSource} at
 * {@code Vec3.atCenterOf(pos)} — which is the technical plot coordinate of the Sable
 * sub-level (e.g. 20 481 032, 128, 20 481 032). That position is far outside the real
 * world, so Plasmo Voice clients never hear the audio because they're filtering by
 * "distance to listener in real-world coords".
 *
 * <p>This class projects the sub-level coordinates back into real-world coordinates so
 * the audio is audible at the platform's actual location in the world, and stays there
 * as the platform moves.
 */
public final class BoomboxSableCompat {
    private static volatile boolean lookedUp;
    private static volatile boolean sableLoaded;
    private static volatile Object helper;
    private static volatile Method projectOutOfSubLevel;

    private BoomboxSableCompat() {
    }

    public static Vec3 projectBoomboxCenter(Level level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        return project(level, center);
    }

    public static Vec3 project(Level level, Vec3 position) {
        // Cheap heuristic: skip Sable probe for normal-world coordinates.
        if (Math.abs(position.x) < 1_000_000 && Math.abs(position.z) < 1_000_000) {
            return position;
        }
        try {
            ensureLookup();
            Object h = helper;
            Method m = projectOutOfSubLevel;
            if (h == null || m == null) {
                return position;
            }
            Object result = m.invoke(h, level, position);
            if (result instanceof Vec3 projected) {
                return projected;
            }
        } catch (Throwable t) {
            if (lookedUp) {
                LazoBoombox.LOGGER.debug("Sable position projection failed: {}", t.toString());
            }
        }
        return position;
    }

    public static boolean isProbablySubLevel(BlockPos pos) {
        return Math.abs(pos.getX()) > 1_000_000 || Math.abs(pos.getZ()) > 1_000_000;
    }

    public static boolean isSableLoaded() {
        if (!lookedUp) ensureLookup();
        return sableLoaded;
    }

    private static void ensureLookup() {
        if (lookedUp) return;
        synchronized (BoomboxSableCompat.class) {
            if (lookedUp) return;
            try {
                Class<?> sable = Class.forName("dev.ryanhcode.sable.Sable");
                Field helperField = sable.getField("HELPER");
                Object h = helperField.get(null);
                Method m = h.getClass().getMethod(
                        "projectOutOfSubLevel",
                        Level.class,
                        net.minecraft.core.Position.class
                );
                helper = h;
                projectOutOfSubLevel = m;
                sableLoaded = true;
                LazoBoombox.LOGGER.info(
                        "LazoBoombox detected Sable; placed boombox positions will be projected " +
                        "to real world coordinates");
            } catch (ClassNotFoundException ignored) {
                sableLoaded = false;
            } catch (Throwable t) {
                sableLoaded = false;
                LazoBoombox.LOGGER.debug(
                        "Sable probe failed; treating as not-installed: {}", t.toString());
            } finally {
                lookedUp = true;
            }
        }
    }
}
