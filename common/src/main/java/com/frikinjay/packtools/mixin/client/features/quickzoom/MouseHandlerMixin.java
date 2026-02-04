package com.frikinjay.packtools.mixin.client.features.quickzoom;

import com.frikinjay.packtools.config.ConfigRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.player.Inventory;
import com.frikinjay.packtools.features.quickzoom.QuickZoomHelper;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(
            at = @At("RETURN"),
            method = "onScroll(JDD)V"
    )
    private void packtools$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!ConfigRegistry.QUICK_ZOOM.getValue()) {
            return;
        }
        QuickZoomHelper.handleScroll(vertical);
    }

    @WrapWithCondition(
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;setSelectedSlot(I)V"),
            method = "onScroll(JDD)V"
    )
    private boolean packtools$preventHotbarScrollOnZoom(Inventory inventory, int slot) {
        if (!ConfigRegistry.QUICK_ZOOM.getValue()) {
            return true;
        }
        return !QuickZoomHelper.shouldPreventHotbarScroll();
    }
}