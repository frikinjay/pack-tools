package com.frikinjay.packtools.mixin.client.features.restocker;

import com.frikinjay.packtools.client.features.restocker.ClientRestockerHelper;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    public LocalPlayerMixin(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Inject(method = "clientSideCloseContainer", at = @At("HEAD"))
    private void packtools$clearOnScreenClose(CallbackInfo ci) {
        ClientRestockerHelper.clear();
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private void packtools$captureBeforeDrop(boolean fullStack, CallbackInfoReturnable<ItemEntity> cir) {
        if (!ConfigRegistry.RESTOCKER.getValue()) return;

        if (!getMainHandItem().isEmpty()) {
            ClientRestockerHelper.scheduleRestockUnchecked(
                    InteractionHand.MAIN_HAND,
                    getInventory(),
                    getMainHandItem().copy(),
                    ItemStack.EMPTY  // After dropping, hand will be empty
            );
        }
    }

    @Inject(method = "drop", at = @At("RETURN"))
    private void packtools$restockAfterDrop(boolean fullStack, CallbackInfoReturnable<ItemEntity> cir) {
        if (!ConfigRegistry.RESTOCKER.getValue()) return;

        ClientRestockerHelper.performRestock();
    }
}