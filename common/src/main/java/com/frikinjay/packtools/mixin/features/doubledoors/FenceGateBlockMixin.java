package com.frikinjay.packtools.mixin.features.doubledoors;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.doubledoors.DoubleDoorsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FenceGateBlock.class)
public abstract class FenceGateBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("TAIL"))
    private void packtools$onGateInteract(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ConfigRegistry.DOUBLE_DOORS.getValue() || player.isShiftKeyDown() || cir.getReturnValue() != InteractionResult.SUCCESS) return;
        BlockState newState = level.getBlockState(pos);
        boolean targetOpen = newState.getValue(BlockStateProperties.OPEN);
        DoubleDoorsHelper.updateFenceGates(level, pos, newState, targetOpen);
    }
}