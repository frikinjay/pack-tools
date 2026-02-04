package com.frikinjay.packtools.mixin.client.features.ping;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AccessibilityOnboardingScreen.class)
public class AccessibilityOnboardingScreenMixin {

    @Inject(
            method = "render",
            at = @At("TAIL")
    )
    private void packTools$accessibilityScreenPing(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (ConfigRegistry.PING.getValue()) {
            if (!PackTools.pinged) {
                Minecraft.getInstance().getSoundManager().playDelayed(new SimpleSoundInstance(SoundEvents.EXPERIENCE_ORB_PICKUP.location(), SoundSource.MASTER, 1.0f, 1.0f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true), 20);
                PackTools.pinged = true;
            }
        }
    }

}
