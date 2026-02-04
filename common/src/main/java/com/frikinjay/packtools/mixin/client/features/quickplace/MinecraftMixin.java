package com.frikinjay.packtools.mixin.client.features.quickplace;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.quickplace.QuickPlaceHelper;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void packtools$updateQuickPlace(CallbackInfo ci) {
        if (!ConfigRegistry.REACHER.getValue()) {
            return;
        }

        QuickPlaceHelper.tick((Minecraft) (Object) this);
    }
}