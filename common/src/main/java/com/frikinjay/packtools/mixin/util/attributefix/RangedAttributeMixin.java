package com.frikinjay.packtools.mixin.util.attributefix;

import com.frikinjay.packtools.platform.CommonPlatformHelper;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RangedAttribute.class)
public abstract class RangedAttributeMixin {

    @Shadow @Final @Mutable private double minValue;
    @Shadow @Final @Mutable private double maxValue;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void packtools$attributeFix(String descriptionId, double defaultValue, double min, double max, CallbackInfo ci) {
        if (!CommonPlatformHelper.isModLoaded("attributefix")) {
            if (descriptionId.endsWith("attribute.name.max_health") ||
                    descriptionId.endsWith("attribute.name.armor") ||
                    descriptionId.endsWith("attribute.name.armor_toughness") ||
                    descriptionId.endsWith("attribute.name.attack_damage") ||
                    descriptionId.endsWith("attribute.name.attack_knockback")
            ) {
                this.maxValue = 1000000.0D;
            }
            if (this.minValue > this.maxValue) {
                this.minValue = this.maxValue;
            }
        }
    }

    @Inject(method = "sanitizeValue", at = @At("HEAD"), cancellable = true)
    private void packtools$preventHealthClamping(double value, CallbackInfoReturnable<Double> cir) {
        if (!CommonPlatformHelper.isModLoaded("attributefix")) {
            if (Double.isNaN(value)) {
                cir.setReturnValue(this.minValue);
            } else {
                cir.setReturnValue(net.minecraft.util.Mth.clamp(value, this.minValue, this.maxValue));
            }
            cir.cancel();
        }
    }
}