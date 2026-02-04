package com.frikinjay.packtools.mixin.client.features.quickzoom;

import com.frikinjay.packtools.config.ConfigRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.GameRenderer;
import com.frikinjay.packtools.features.quickzoom.QuickZoomHelper;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @ModifyReturnValue(
            at = @At("RETURN"),
            method = "getFov(Lnet/minecraft/client/Camera;FZ)F"
    )
    private float packtools$modifyFovForZoom(float fov) {
        if (!ConfigRegistry.QUICK_ZOOM.getValue()) {
            return fov;
        }
        return QuickZoomHelper.modifyFov(fov);
    }
}