package com.frikinjay.packtools.mixin.features.rapidpiglin;

import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PiglinAi.class)
public class PiglinAiMixin {

    @Unique
    private static int NEW_ADMIRE_DURATION = ConfigRegistry.PIGLIN_TRADE_DURATION.getValue();

    @Final
    @Shadow
    @Mutable
    private static int ADMIRE_DURATION;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void packtools$updateField(CallbackInfo ci) {
        if (ConfigRegistry.RAPID_PIGLIN.getValue()) {
            ADMIRE_DURATION = NEW_ADMIRE_DURATION;
        }
    }

    @ModifyConstant(
            method = "initCoreActivity",
            constant = @Constant(intValue = 119)
    )
    private static int packtools$modifyAdmireDurationInt(int constant) {
        if (ConfigRegistry.RAPID_PIGLIN.getValue()) {
            return NEW_ADMIRE_DURATION;
        }
        return 119;
    }

    @ModifyConstant(
            method = "admireGoldItem",
            constant = @Constant(longValue = 119L)
    )
    private static long packtools$modifyAdmireDurationLong(long constant) {
        if (ConfigRegistry.RAPID_PIGLIN.getValue()) {
            return (long) NEW_ADMIRE_DURATION;
        }
        return 119L;
    }
}