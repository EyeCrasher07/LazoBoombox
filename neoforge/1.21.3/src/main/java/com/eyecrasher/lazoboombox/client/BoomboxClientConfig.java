package com.eyecrasher.lazoboombox.client;
import com.eyecrasher.lazoboombox.LazoBoombox;
import net.neoforged.fml.loading.FMLPaths;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;

public final class BoomboxClientConfig {
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve(LazoBoombox.MOD_ID);
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("client.toml");
    public enum HudPosition { TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT, CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT }
    private static volatile HudPosition position = HudPosition.TOP_RIGHT;
    private static volatile int xOffset = 8, yOffset = 8;
    private static volatile boolean timerVisible = true;
    private BoomboxClientConfig() {}
    public static void load() {
        try { Files.createDirectories(CONFIG_DIR); if (Files.notExists(CONFIG_FILE)) { writeDefault(); return; }
            try (BufferedReader r = Files.newBufferedReader(CONFIG_FILE)) { String line;
                while ((line = r.readLine()) != null) { String t = line.trim(); if (t.isEmpty() || t.startsWith("#")) continue; int eq = t.indexOf('='); if (eq <= 0) continue; String key = t.substring(0, eq).trim(), val = t.substring(eq+1).trim();
                    switch (key) { case "timer_position" -> position = parsePosition(val, position); case "timer_x_offset" -> xOffset = parseInt(val, xOffset, -1000, 1000); case "timer_y_offset" -> yOffset = parseInt(val, yOffset, -1000, 1000); case "timer_visible" -> timerVisible = parseBool(val, timerVisible); } } }
        } catch (Exception e) { LazoBoombox.LOGGER.warn("Could not load client config: {}", e.toString()); }
    }
    public static HudPosition getPosition() { return position; }
    public static int getXOffset() { return xOffset; }
    public static int getYOffset() { return yOffset; }
    public static boolean isTimerVisible() { return timerVisible; }
    private static void writeDefault() throws Exception { try (BufferedWriter w = Files.newBufferedWriter(CONFIG_FILE)) { w.write("# LazoBoombox client config\ntimer_position = \"top_right\"\n\ntimer_x_offset = 8\ntimer_y_offset = 8\n\ntimer_visible = true\n"); } }
    private static HudPosition parsePosition(String val, HudPosition fallback) { try { return HudPosition.valueOf(val.toUpperCase(Locale.ROOT)); } catch (Exception e) { return fallback; } }
    private static int parseInt(String val, int fallback, int min, int max) { try { return Math.max(min, Math.min(max, Integer.parseInt(val))); } catch (Exception e) { return fallback; } }
    private static boolean parseBool(String val, boolean fallback) { String t = val.toLowerCase(Locale.ROOT); if (t.equals("true") || t.equals("yes") || t.equals("1")) return true; if (t.equals("false") || t.equals("no") || t.equals("0")) return false; return fallback; }
}
