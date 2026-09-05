package com.eyecrasher.lazoboombox.config;
import com.eyecrasher.lazoboombox.LazoBoombox; import net.fabricmc.loader.api.FabricLoader;
import java.io.*; import java.nio.file.*; import java.util.*;
public final class BoomboxConfig {
    private static final List<Value<?>> VALUES = new ArrayList<>();
    public static final BooleanValue FEATURE_PROGRESS_BAR = bool("features.progress_bar", true);
    public static final BooleanValue FEATURE_PLACED_BOOMBOX = bool("features.placed_boombox", true);
    public static final BooleanValue FEATURE_OWNERSHIP = bool("features.ownership", true);
    public static final DoubleValue BOOMBOX_VOLUME = doub("boombox.volume", 1.0D, 0.0D, 4.0D);
    public static final IntValue BOOMBOX_RADIUS = intr("boombox.radius", 32, 1, 512);
    public static final BooleanValue BOOMBOX_RADIUS_FOLLOWS_VOLUME = bool("boombox.radius_follows_volume", true);
    public static final IntValue BOOMBOX_MAX_RADIUS = intr("boombox.max_radius", 128, 1, 1024);
    public static final IntValue BOOMBOX_MAX_CONCURRENT_SOURCES = intr("boombox.max_concurrent_sources", 16, 1, 256);
    public static final BooleanValue OWNERSHIP_ONLY_OWNER_PICKUP = bool("ownership.only_owner_can_pickup", true);
    public static final IntValue PROGRESS_BAR_Y_OFFSET = intr("progress_bar.y_offset", 8, -200, 400);
    private BoomboxConfig() {}
    public static void load() {
        try { Files.createDirectories(configDir()); Path file = configFile(); if (Files.notExists(file)) writeDefault(file); read(file); }
        catch (Exception e) { LazoBoombox.LOGGER.warn("Could not load LazoBoombox config: {}", e.toString()); }
    }
    public static Path configDir() { return FabricLoader.getInstance().getConfigDir().resolve(LazoBoombox.MOD_ID); }
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
            w.write("# LazoBoombox configuration\n\n[features]\nprogress_bar = true\nplaced_boombox = true\nownership = true\n\n");
            w.write("[boombox]\nvolume = 1.0\nradius = 32\nradius_follows_volume = true\nmax_radius = 128\nmax_concurrent_sources = 16\n\n");
            w.write("[ownership]\nonly_owner_can_pickup = true\n\n[progress_bar]\ny_offset = 8\n");
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
