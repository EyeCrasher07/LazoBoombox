package com.eyecrasher.lazoboombox.client;
import com.eyecrasher.lazoboombox.data.BoomboxData;
import com.eyecrasher.lazoboombox.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class BoomboxHudOverlay {
    private static volatile boolean active = false;
    private static volatile long positionMs = 0L, durationMs = 0L;

    public BoomboxHudOverlay() {}

    public static void setState(boolean active, long positionMs, long durationMs) {
        BoomboxHudOverlay.active = active;
        BoomboxHudOverlay.positionMs = positionMs;
        BoomboxHudOverlay.durationMs = durationMs;
    }

    public static void onRender(RenderGuiEvent.Post event) {
        render(event.getGuiGraphics());
    }

    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.font == null) return;
        if (!BoomboxClientConfig.isTimerVisible()) return;
        ItemStack held = heldBoombox(mc);
        if (held == null || !BoomboxData.hasDisc(held) || !active) return;
        String timeText = formatTime(positionMs) + " / " + formatTime(durationMs);
        int textWidth = mc.font.width(timeText);
        int sw = graphics.guiWidth(), sh = graphics.guiHeight();
        int xo = BoomboxClientConfig.getXOffset(), yo = BoomboxClientConfig.getYOffset();
        int x, y;
        BoomboxClientConfig.HudPosition pos = BoomboxClientConfig.getPosition();
        switch (pos) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> x = xo;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> x = (sw - textWidth) / 2 + xo;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> x = sw - textWidth - xo;
            default -> x = sw - textWidth - xo;
        }
        switch (pos) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> y = yo;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> y = (sh - mc.font.lineHeight) / 2 + yo;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> y = sh - mc.font.lineHeight - yo;
            default -> y = yo;
        }
        graphics.fill(x - 8, y - 4, x - 8 + textWidth + 16, y - 4 + mc.font.lineHeight + 8, 0x80000000);
        graphics.drawString(mc.font, Component.literal(timeText), x, y, 0xFFFFFFFF, false);
    }

    private static String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000L;
        return String.format("%02d:%02d", s / 60L, s % 60L);
    }

    private static ItemStack heldBoombox(Minecraft mc) {
        ItemStack m = mc.player.getMainHandItem();
        if (m.getItem() == ModBlocks.BOOMBOX_ITEM.get()) return m;
        ItemStack o = mc.player.getOffhandItem();
        if (o.getItem() == ModBlocks.BOOMBOX_ITEM.get()) return o;
        return null;
    }
}
