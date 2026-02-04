package com.frikinjay.packtools.mixin.client.features.swingthrough;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.swingthrough.SwingThroughHelper;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

import static com.frikinjay.packtools.features.swingthrough.SwingThroughHelper.STASHED_BLOCK_HIT;

@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @Inject(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At("HEAD"))
    private static void packtools$clearStashOnStart(Entity entity, double d, double e, float f, CallbackInfoReturnable<HitResult> cir) {
        if (ConfigRegistry.SWING_THROUGH.getValue()) {
            STASHED_BLOCK_HIT.remove();
        }
    }

    @WrapOperation(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;")
    )
    private static HitResult packtools$allowPickThroughTransparent(Entity entity, double dist, float tick, boolean fluids, Operation<HitResult> original) {
        if (ConfigRegistry.SWING_THROUGH.getValue()) {
            HitResult result = original.call(entity, dist, tick, fluids);
            if (SwingThroughHelper.isSwingThroughBlock(result)) {
                STASHED_BLOCK_HIT.set(result);
                Vec3 farPos = entity.getEyePosition(tick).add(entity.getViewVector(tick).scale(dist));
                return BlockHitResult.miss(farPos, Direction.UP, BlockPos.containing(farPos));
            }
            return result;
        }
        return original.call(entity, dist, tick, fluids);
    }

    @WrapOperation(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ProjectileUtil;getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;")
    )
    private static EntityHitResult packtools$applyEnhancedFilter(Entity shooter, Vec3 start, Vec3 end, AABB box, Predicate<Entity> filter, double maxDistSqr, Operation<EntityHitResult> original) {
        if (ConfigRegistry.SWING_THROUGH.getValue()) {
            if (STASHED_BLOCK_HIT.get() == null) {
                return original.call(shooter, start, end, box, filter, maxDistSqr);
            }
            Predicate<Entity> enhancedFilter = entity ->
                    filter.test(entity) && SwingThroughHelper.isValidTarget(entity, shooter, start, maxDistSqr);
            return original.call(shooter, start, end, box, enhancedFilter, maxDistSqr);
        }
        return original.call(shooter, start, end, box, filter, maxDistSqr);
    }

    @Inject(
            method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void packtools$restoreBlockHitIfNoEntity(Entity entity, double d, double e, float f, CallbackInfoReturnable<HitResult> cir) {
        if (ConfigRegistry.SWING_THROUGH.getValue()) {
            try {
                HitResult currentResult = cir.getReturnValue();
                HitResult stashed = STASHED_BLOCK_HIT.get();
                if (stashed != null && (currentResult == null || currentResult.getType() == HitResult.Type.MISS)) {
                    cir.setReturnValue(stashed);
                }
            } finally {
                STASHED_BLOCK_HIT.remove();
            }
        }
    }
}