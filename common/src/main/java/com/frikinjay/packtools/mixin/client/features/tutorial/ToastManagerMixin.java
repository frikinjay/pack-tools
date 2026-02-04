package com.frikinjay.packtools.mixin.client.features.tutorial;

import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin {

    @Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
    private void packtools$addToast(Toast toast, CallbackInfo ci) {
        if (ConfigRegistry.TOAST_SUPPRESSOR.getValue()) {
            if (toast instanceof RecipeToast || toast instanceof TutorialToast) {
                ci.cancel();
            }
        }
    }
}
