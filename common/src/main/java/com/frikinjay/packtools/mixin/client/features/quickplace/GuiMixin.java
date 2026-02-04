package com.frikinjay.packtools.mixin.client.features.quickplace;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.quickplace.QuickPlaceRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Shadow @Final
    private Minecraft minecraft;

    @Inject(method = "render", at = @At("TAIL"))
    private void packtools$renderQuickPlaceIndicator(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!ConfigRegistry.REACHER.getValue()) {
            return;
        }

        if (this.minecraft.screen != null) {
            return;
        }

        QuickPlaceRenderer.render2D(guiGraphics, this.minecraft);
    }
}