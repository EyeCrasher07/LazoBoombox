package com.eyecrasher.lazoboombox.config;
import com.eyecrasher.lazoboombox.LazoBoombox; import net.neoforged.fml.loading.FMLPaths;
import java.io.*; import java.nio.file.*; import java.util.*;
public final class BoomboxConfig {
    private static final List<Value<?>> VALUES = new ArrayList<>();
    public static final BooleanValue FEATURE_PLACED_BOOMBOX = bool("features.placed_boombox", true);
    public static final BooleanValue FEATURE_OWNERSHIP = bool("features.ownership", false);
    /** EXPERIMENTAL — disabled by default, see writeDefault() comment below. */
    public static final BooleanValue ALLOW_PLAYBACK_ON_SABLE_PLATFORMS = bool("features.allowPlaybackOnSablePlatforms", false);
    public static final DoubleValue BOOMBOX_VOLUME = doub("boombox.volume", 1.0D, 0.0D, 4.0D);
    public static final BooleanValue BOOMBOX_RADIUS_FOLLOWS_VOLUME = bool("boombox.radius_follows_volume", true);
    public static final IntValue BOOMBOX_MAX_RADIUS = intr("boombox.max_radius", 128, 1, 1024);
    public static final BooleanValue OWNERSHIP_ONLY_OWNER_PICKUP = bool("ownership.only_owner_can_pickup", true);
    private BoomboxConfig() {}
    public static void load() {
        try { Files.createDirectories(configDir()); Path file = configFile(); if (Files.notExists(file)) writeDefault(file); read(file); }
        catch (Exception e) { LazoBoombox.LOGGER.warn("Could not load LazoBoombox config: {}", e.toString()); }
    }
    public static Path configDir() { return FMLPaths.CONFIGDIR.get().resolve(LazoBoombox.MOD_ID); }
    public static Path configFile() { return configDir().resolve("config.toml"); }
    private static void read(Path file) throws Exception {
        String section = ""; try (BufferedReader r = Files.newBufferedReader(file)) { String line;
            while ((line = r.readLine()) != null) { String t = line.trim(); if (t.isEmpty() || t.startsWith("#")) continue;
                if (t.startsWith("[") && t.endsWith("]")) { section = t.substring(1, t.length()-1).trim(); continue; }
                int eq = t.indexOf('='); if (eq <= 0) continue;
                String key = t.substring(0, eq).trim(), path = section.isBlank() ? key : section + "." + key, raw = t.substring(eq+1).trim();
                for (Value<?> v : VALUES) if (v.path.equals(path)) { v.read(raw); break; }
            }
        }
    }
    private static void writeDefault(Path file) throws Exception {
        try (BufferedWriter w = Files.newBufferedWriter(file)) {
            w.write("# LazoBoombox server configuration\n");
            w.write("# This file controls server-side boombox behavior. Client-side HUD settings\n");
            w.write("# (timer position, visibility) are in client.toml.\n\n");
            w.write("[features]\n");
            w.write("# Allow players to place boomboxes as blocks in the world (not just carry them).\n");
            w.write("placed_boombox = true\n\n");
            w.write("# Track boombox ownership. When false, any player can pick up any boombox.\n");
            w.write("# When true, only the owner can pick up their own boombox (see ownership.only_owner_can_pickup).\n");
            w.write("ownership = false\n\n");
            w.write("# EXPERIMENTAL: Allow playback on Sable moving platforms (Create Aeronautics airships).\n");
            w.write("# When false, boomboxes on moving platforms refuse to play (position is in technical sub-level).\n");
            w.write("# When true, position is projected to real-world coordinates and updated every physics tick (~60 Hz).\n");
            w.write("allowPlaybackOnSablePlatforms = false\n\n");
            w.write("[boombox]\n");
            w.write("# Global volume multiplier for all boomboxes (0.0 = silent, 1.0 = normal, 4.0 = very loud).\n");
            w.write("# Final volume = disc.volume (from /lazodisc burn) * boombox.volume.\n");
            w.write("volume = 1.0\n\n");
            w.write("# If true, effective hearing radius scales with volume (quieter = smaller radius).\n");
            w.write("# If false, radius comes only from the disc's range setting.\n");
            w.write("radius_follows_volume = true\n\n");
            w.write("# Hard cap on hearing radius (blocks). Even if a disc has range=512, effective radius = min(512, max_radius).\n");
            w.write("max_radius = 128\n\n");
            w.write("[ownership]\n");
            w.write("# Only has effect when features.ownership = true.\n");
            w.write("# If true, only the boombox owner (UUID recorded when placed) can pick it up with Shift+RMB.\n");
            w.write("# If false, any player can pick up any owned boombox.\n");
            w.write("only_owner_can_pickup = true\n");
        }
    }
    private static Value<?> register(Value<?> v) { VALUES.add(v); return v; }
    private static IntValue intr(String p, int d, int min, int max) { return (IntValue) register(new IntValue(p, d, min, max)); }
    private static DoubleValue doub(String p, double d, double min, double max) { return (DoubleValue) register(new DoubleValue(p, d, min, max)); }
    private static BooleanValue bool(String p, boolean d) { return (BooleanValue) register(new BooleanValue(p, d)); }
    public abstract static class Value<T> { final String path; T value; Value(String p, T d) { path = p; value = d; } abstract void read(String raw); }
    public static final class IntValue extends Value<Integer> { final int min, max; IntValue(String p, int d, int mn, int mx) { super(p, d); min = mn; max = mx; } void read(String r) { try { value = Math.max(min, Math.min(max, Integer.parseInt(r))); } catch (Exception e) {} } public int get() { return value; } }
    public static final class DoubleValue extends Value<Double> { final double min, max; DoubleValue(String p, double d, double mn, double mx) { super(p, d); min = mn; max = mx; } void read(String r) { try { value = Math.max(min, Math.min(max, Double.parseDouble(r))); } catch (Exception e) {} } public double get() { return value; } }
    public static final class BooleanValue extends Value<Boolean> { BooleanValue(String p, boolean d) { super(p, d); } void read(String r) { String t = r.toLowerCase(Locale.ROOT); value = t.equals("true") || t.equals("yes") || t.equals("1"); } public boolean get() { return value; } }
}
