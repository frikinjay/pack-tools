package com.frikinjay.packtools.mixin.client.features.quickplace;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.quickplace.QuickPlaceHelper;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void packtools$quickPlaceOnUseItem(
            Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {

        if (!ConfigRegistry.REACHER.getValue()) {
            return;
        }

        if (!QuickPlaceHelper.hasActiveTarget()) {
            return;
        }

        InteractionHand targetHand = QuickPlaceHelper.getTargetHand();
        if (targetHand != interactionHand) {
            return;
        }

        ItemStack stack = player.getItemInHand(interactionHand);
        if (!QuickPlaceHelper.shouldAttemptQuickPlace(stack)) {
            return;
        }

        BlockHitResult quickPlaceHit = QuickPlaceHelper.createPlacementHit((LocalPlayer) player);
        if (quickPlaceHit == null) {
            return;
        }

        MultiPlayerGameMode self = (MultiPlayerGameMode) (Object) this;
        InteractionResult result = self.useItemOn((LocalPlayer) player, interactionHand, quickPlaceHit);

        if (result.consumesAction()) {
            cir.setReturnValue(result);
        }
    }
}