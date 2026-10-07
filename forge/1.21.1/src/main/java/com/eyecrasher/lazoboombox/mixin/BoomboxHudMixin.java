package com.eyecrasher.lazoboombox.mixin;

import com.eyecrasher.lazoboombox.client.BoomboxHudOverlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class BoomboxHudMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void lazoboombox$renderTimer(
            GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        BoomboxHudOverlay.render(graphics);
    }
}
