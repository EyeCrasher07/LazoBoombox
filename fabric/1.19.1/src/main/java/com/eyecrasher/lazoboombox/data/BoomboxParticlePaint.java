package com.eyecrasher.lazoboombox.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Short-lived client paint snapshots for particles arriving after block-entity removal. */
public final class BoomboxParticlePaint {
    private static final int UNPAINTED = -1;
    private static final int MAX_POSITIONS = 128;
    private static final long LIFETIME_NANOS = 5_000_000_000L;
    private static final Map<Object, LinkedHashMap<Long, Paint>> WORLDS = new WeakHashMap<>();

    private BoomboxParticlePaint() {}

    public static void remember(Object world, long position, int color) {
        rememberAt(world, position, color, System.nanoTime());
    }

    public static int read(Object world, long position) {
        return readAt(world, position, System.nanoTime());
    }

    static synchronized void rememberAt(Object world, long position, int color, long now) {
        if (world == null) return;
        LinkedHashMap<Long, Paint> paints =
                WORLDS.computeIfAbsent(world, ignored -> new LinkedHashMap<>());
        paints.entrySet().removeIf(entry -> now - entry.getValue().createdAt >= LIFETIME_NANOS);
        paints.remove(position);
        paints.put(position, new Paint(color >= 0 && color <= 0xFFFFFF ? color : UNPAINTED, now));
        if (paints.size() > MAX_POSITIONS) paints.remove(paints.keySet().iterator().next());
    }

    static synchronized int readAt(Object world, long position, long now) {
        Map<Long, Paint> paints = WORLDS.get(world);
        if (paints == null) return UNPAINTED;
        Paint paint = paints.get(position);
        if (paint == null) return UNPAINTED;
        if (now - paint.createdAt >= LIFETIME_NANOS) {
            paints.remove(position);
            if (paints.isEmpty()) WORLDS.remove(world);
            return UNPAINTED;
        }
        return paint.color;
    }

    private static final class Paint {
        final int color;
        final long createdAt;

        Paint(int color, long createdAt) {
            this.color = color;
            this.createdAt = createdAt;
        }
    }
}
