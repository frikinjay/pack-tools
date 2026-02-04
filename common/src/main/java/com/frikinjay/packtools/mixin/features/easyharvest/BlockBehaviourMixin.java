package com.frikinjay.packtools.mixin.features.easyharvest;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.easyharvest.EasyHarvestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockBehaviour.class, priority = 900)
public class BlockBehaviourMixin {

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void packtools$onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                  BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!ConfigRegistry.EASY_HARVEST.getValue()) return;
        if (player.isCrouching() || player.isSpectator()) return;

        Block block = state.getBlock();
        boolean shouldHarvest = false;
        boolean isTallPlant = false;

        if (state.is(EasyHarvestHelper.HARVEST_BLACKLIST)) return;

        if (state.is(EasyHarvestHelper.TALL_HARVESTABLE)) {
            shouldHarvest = true;
            isTallPlant = true;
        }

        else if (block instanceof CropBlock crop) {
            shouldHarvest = crop.isMaxAge(state);
        } else if (block instanceof CocoaBlock) {
            shouldHarvest = state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        } else if (block instanceof NetherWartBlock) {
            shouldHarvest = state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }

        if (!shouldHarvest) return;

        boolean harvested = false;
        if (isTallPlant) {
            harvested = EasyHarvestHelper.harvestTallPlant(level, pos, state, player, InteractionHand.MAIN_HAND, hit.getDirection());
        } else {
            harvested = EasyHarvestHelper.harvestCrop(level, pos, state, player, InteractionHand.MAIN_HAND, true);
        }

        if (harvested) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}