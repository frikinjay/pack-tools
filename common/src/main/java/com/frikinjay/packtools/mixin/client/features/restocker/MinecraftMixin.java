package com.frikinjay.packtools.mixin.client.features.restocker;

import com.frikinjay.packtools.client.features.restocker.ClientRestockerHelper;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public LocalPlayer player;

    @Unique
    private ItemStack packtools$mainHandStack;

    @Unique
    private ItemStack packtools$offHandStack;

    @Inject(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/InteractionHand;values()[Lnet/minecraft/world/InteractionHand;"))
    private void packtools$captureBeforeUse(CallbackInfo ci) {
        if (!ConfigRegistry.RESTOCKER.getValue() || player == null) return;

        packtools$mainHandStack = player.getMainHandItem();
        packtools$offHandStack = player.getOffhandItem();

        packtools$mainHandStack = packtools$mainHandStack.isEmpty() ? null : packtools$mainHandStack.copy();
        packtools$offHandStack = packtools$offHandStack.isEmpty() ? null : packtools$offHandStack.copy();
    }

    @Inject(method = "startUseItem", at = @At("RETURN"))
    private void packtools$restockAfterUse(CallbackInfo ci) {
        if (!ConfigRegistry.RESTOCKER.getValue() || player == null) return;

        boolean restockScheduled = false;

        if (packtools$mainHandStack != null) {
            restockScheduled = ClientRestockerHelper.scheduleRestockChecked(
                    InteractionHand.MAIN_HAND,
                    player.getInventory(),
                    packtools$mainHandStack,
                    player.getMainHandItem()
            );
        }

        if (!restockScheduled && packtools$offHandStack != null) {
            ClientRestockerHelper.scheduleRestockChecked(
                    InteractionHand.OFF_HAND,
                    player.getInventory(),
                    packtools$offHandStack,
                    player.getOffhandItem()
            );
        }

        ClientRestockerHelper.performRestock();

        packtools$mainHandStack = null;
        packtools$offHandStack = null;
    }
}