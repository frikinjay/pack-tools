package com.frikinjay.packtools.mixin.client.features.restocker;

import com.frikinjay.packtools.client.features.restocker.ClientRestockerHelper;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {
    @Shadow
    public abstract Slot getSlot(int index);

    @Inject(method = "initializeContents",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;set(Lnet/minecraft/world/item/ItemStack;)V",
                    shift = At.Shift.BEFORE),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private void packtools$scheduleRestockOnSlotUpdate(int syncId, List<ItemStack> stacks,
                                                       ItemStack cursorStack, CallbackInfo ci, int index) {
        //noinspection ConstantConditions
        if (!ConfigRegistry.RESTOCKER.getValue() || !((Object) this instanceof InventoryMenu)) {
            return;
        }

        if (ClientRestockerHelper.isProcessingServerUpdate()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Inventory playerInventory = client.player.getInventory();
        Slot targetSlot = getSlot(index);

        if (targetSlot.container != playerInventory) return;

        int indexInInv = targetSlot.getContainerSlot();
        int selectedSlot = playerInventory.getSelectedSlot();

        ItemStack beforeStack = playerInventory.getItem(indexInInv);
        ItemStack afterStack = stacks.get(index);

        if (indexInInv == selectedSlot) {
            ClientRestockerHelper.scheduleRestockChecked(
                    InteractionHand.MAIN_HAND,
                    playerInventory,
                    beforeStack,
                    afterStack
            );
        } else if (indexInInv == Inventory.SLOT_OFFHAND) {
            ClientRestockerHelper.scheduleRestockChecked(
                    InteractionHand.OFF_HAND,
                    playerInventory,
                    beforeStack,
                    afterStack
            );
        }
    }

    @Inject(method = "initializeContents",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/inventory/Slot;set(Lnet/minecraft/world/item/ItemStack;)V",
                    shift = At.Shift.AFTER))
    private void packtools$performRestockAfterSlotUpdate(int syncId, List<ItemStack> stacks,
                                                         ItemStack cursorStack, CallbackInfo ci) {
        if (!ConfigRegistry.RESTOCKER.getValue()) return;

        ClientRestockerHelper.performRestock();
    }

    @Inject(method = "initializeContents", at = @At("HEAD"))
    private void packtools$markServerUpdateStart(int syncId, List<ItemStack> stacks,
                                                 ItemStack cursorStack, CallbackInfo ci) {
        if (!ConfigRegistry.RESTOCKER.getValue()) return;
        ClientRestockerHelper.setProcessingServerUpdate(true);
    }

    @Inject(method = "initializeContents", at = @At("RETURN"))
    private void packtools$markServerUpdateEnd(int syncId, List<ItemStack> stacks,
                                               ItemStack cursorStack, CallbackInfo ci) {
        if (!ConfigRegistry.RESTOCKER.getValue()) return;
        ClientRestockerHelper.setProcessingServerUpdate(false);
    }
}