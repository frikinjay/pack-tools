package com.frikinjay.packtools.mixin.util.attributefix;

import com.frikinjay.packtools.util.attributefix.IHealthRestorePoint;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 9001)
public abstract class LivingEntityMixin implements IHealthRestorePoint {

    @Unique
    @Nullable
    private Float maxhealthfix$restorePoint = null;

    @Override
    public void packtools$setRestorePoint(Float restorePoint) {
        this.maxhealthfix$restorePoint = restorePoint;
    }

    @ModifyArg(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"),
            index = 0
    )
    private float packtools$getSavedHealth(float savedHealth) {
        if (savedHealth > this.getMaxHealth() && savedHealth > 0) {
            this.packtools$setRestorePoint(savedHealth);
        }
        return savedHealth;
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void packtools$tick(CallbackInfo callback) {
        if (this.maxhealthfix$restorePoint != null) {
            this.setHealth(this.maxhealthfix$restorePoint);
            this.packtools$setRestorePoint(null);
        }
    }

    @Shadow
    public abstract float getMaxHealth();

    @Shadow
    public abstract void setHealth(float newHealth);
}