package com.frikinjay.packtools.mixin.client.features.quickzoom;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import com.frikinjay.packtools.features.quickzoom.QuickZoomHelper;
import java.util.Arrays;

@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(at = @At("RETURN"), method = "<init>")
    private void packtools$onInit(CallbackInfo ci) {
        KeyMapping zoomKey = QuickZoomHelper.createZoomKey();
        keyMappings = Arrays.copyOf(keyMappings, keyMappings.length + 1);
        keyMappings[keyMappings.length - 1] = zoomKey;
    }
}
