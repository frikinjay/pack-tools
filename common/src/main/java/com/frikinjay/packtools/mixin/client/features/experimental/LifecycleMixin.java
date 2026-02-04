package com.frikinjay.packtools.mixin.client.features.experimental;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.serialization.Lifecycle;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Lifecycle.class)
public class LifecycleMixin {

    @Shadow
    @Final
    private static Lifecycle STABLE;

    @Shadow
    @Final
    private static Lifecycle EXPERIMENTAL;

    @WrapMethod(method = "experimental")
    private static Lifecycle packTools$disableExperimentalMessage(Operation<Lifecycle> operation) {
        if (ConfigRegistry.EXPERIMENTAL_SUPPRESSOR.getValue()) {
            return STABLE;
        }
        return EXPERIMENTAL;
    }

}
