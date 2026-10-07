package com.eyecrasher.lazoboombox.compat;

import com.eyecrasher.lazoboombox.LazoBoombox;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Optional Sable position projection through reflection, with a normal-world fallback. */
public final class BoomboxSableCompat {
    private static volatile boolean lookedUp;
    private static volatile boolean sableLoaded;
    private static volatile Object helper;
    private static volatile Method projectOutOfSubLevel;

    private BoomboxSableCompat() {}

    public static Vector3d projectBoomboxCenter(World level, BlockPos pos) {
        Vector3d center = Vector3d.atCenterOf(pos);
        return project(level, center);
    }

    public static Vector3d project(World level, Vector3d position) {
        try {
            ensureLookup();
            Object h = helper;
            Method m = projectOutOfSubLevel;
            if (h == null || m == null) {
                return position;
            }
            Object result = m.invoke(h, level, position);
            if (result instanceof Vector3d projected) {
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
        return isSableLoaded()
                && (Math.abs(pos.getX()) > 1_000_000 || Math.abs(pos.getZ()) > 1_000_000);
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
                Method m =
                        h.getClass()
                                .getMethod(
                                        "projectOutOfSubLevel",
                                        World.class,
                                        net.minecraft.dispenser.IPosition.class);
                helper = h;
                projectOutOfSubLevel = m;
                sableLoaded = true;
                LazoBoombox.LOGGER.info(
                        "LazoBoombox detected Sable; placed boombox positions will be projected "
                                + "to real world coordinates");
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
