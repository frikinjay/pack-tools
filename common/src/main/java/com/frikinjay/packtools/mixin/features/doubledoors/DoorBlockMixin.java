package com.frikinjay.packtools.mixin.features.doubledoors;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.doubledoors.DoubleDoorsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    @Inject(method = "useWithoutItem", at = @At("TAIL"))
    private void packtools$onPlayerInteract(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ConfigRegistry.DOUBLE_DOORS.getValue() || player.isShiftKeyDown() || cir.getReturnValue() != InteractionResult.SUCCESS) return;
        BlockState newState = level.getBlockState(pos);
        boolean targetOpen = newState.getValue(DoorBlock.OPEN);
        BlockPos siblingPos = DoubleDoorsHelper.getMatchingDoor(level, pos, newState);
        if (siblingPos != null) {
            BlockState siblingState = level.getBlockState(siblingPos);
            if (siblingState.getValue(DoorBlock.OPEN) != targetOpen) {
                level.setBlock(siblingPos, siblingState.setValue(DoorBlock.OPEN, targetOpen), 10);
            }
        }
    }

    @Inject(method = "setOpen", at = @At("HEAD"))
    private void packtools$onEntityInteract(Entity entity, Level level, BlockState state, BlockPos pos, boolean open, CallbackInfo ci) {
        if (!ConfigRegistry.DOUBLE_DOORS.getValue() || level.isClientSide()) return;
        BlockPos siblingPos = DoubleDoorsHelper.getMatchingDoor(level, pos, state);
        if (siblingPos != null) {
            BlockState siblingState = level.getBlockState(siblingPos);
            if (siblingState.getValue(DoorBlock.OPEN) != open) {
                level.setBlock(siblingPos, siblingState.setValue(DoorBlock.OPEN, open), 10);
            }
        }
    }
}