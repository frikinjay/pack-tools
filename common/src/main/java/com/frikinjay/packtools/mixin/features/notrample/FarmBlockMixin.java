package com.frikinjay.packtools.mixin.features.notrample;

import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FarmBlock.class)
public class FarmBlockMixin {

    @Redirect(
            method = "fallOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/FarmBlock;turnToDirt(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
            )
    )
    private void packtools$cancelTurnToDirt(@Nullable Entity entity, BlockState blockState, Level level, BlockPos blockPos) {
        if (!ConfigRegistry.NO_TRAMPLE.getValue()) {
            FarmBlock.turnToDirt(entity, blockState, level, blockPos);
        }
    }
}