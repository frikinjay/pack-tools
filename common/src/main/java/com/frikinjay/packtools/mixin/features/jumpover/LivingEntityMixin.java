package com.frikinjay.packtools.mixin.features.jumpover;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.jumpover.JumpOverHelper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(at = @At("TAIL"), method = "jumpFromGround()V")
    private void packtools$jumpOver(CallbackInfo info) {
        if (!ConfigRegistry.JUMP_OVER.getValue()) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().isClientSide()) return;
        if (!(entity instanceof LocalPlayer player)) return;
        if (JumpOverHelper.nearJumpableBlock(player)) {
            player.setDeltaMovement(player.getDeltaMovement().add(0.0, 0.05, 0.0));
        }
    }
}