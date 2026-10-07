package com.eyecrasher.lazoboombox.data;

import net.minecraft.world.item.DyeColor;

import java.util.List;

/** Vanilla leather-style RGB averaging, preserving the average dye brightness. */
public final class BoomboxDyeColors {
    private BoomboxDyeColors() {}

    public static int mix(int previous, List<DyeColor> dyes) {
        if (dyes.isEmpty()) return previous;
        int redSum = 0, greenSum = 0, blueSum = 0, brightnessSum = 0, count = 0;
        if (BoomboxData.isValidColor(previous)) {
            int red = (previous >> 16) & 255, green = (previous >> 8) & 255, blue = previous & 255;
            redSum += red;
            greenSum += green;
            blueSum += blue;
            brightnessSum += Math.max(red, Math.max(green, blue));
            count++;
        }
        for (DyeColor dye : dyes) {
            float[] color = dye.getTextureDiffuseColors();
            int red = (int) (color[0] * 255.0F);
            int green = (int) (color[1] * 255.0F);
            int blue = (int) (color[2] * 255.0F);
            redSum += red;
            greenSum += green;
            blueSum += blue;
            brightnessSum += Math.max(red, Math.max(green, blue));
            count++;
        }
        int red = redSum / count, green = greenSum / count, blue = blueSum / count;
        int maximum = Math.max(red, Math.max(green, blue));
        if (maximum == 0) return 0;
        float brightness = (float) brightnessSum / count;
        red = (int) (red * brightness / maximum);
        green = (int) (green * brightness / maximum);
        blue = (int) (blue * brightness / maximum);
        return (red << 16) | (green << 8) | blue;
    }
}
