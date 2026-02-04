package com.frikinjay.packtools.mixin.client.features.restocker;

import com.frikinjay.packtools.client.features.restocker.ClientRestockerHelper;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract InteractionHand getUsedItemHand();

    @Shadow
    protected ItemStack useItem;

    @Unique
    private ItemStack packtools$itemBeforeFinish;

    @Unique
    private ItemStack packtools$itemReturnedFromFinish;

    @Inject(method = "completeUsingItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private void packtools$captureBeforeConsume(CallbackInfo ci) {
        if (ConfigRegistry.RESTOCKER.getValue()) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity.level().isClientSide() && entity instanceof Player) {
                this.packtools$itemBeforeFinish = this.useItem.copy();
            }
        }
    }

    @ModifyVariable(
            method = "completeUsingItem",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"),
            ordinal = 0
    )
    private ItemStack packtools$captureFinishResult(ItemStack returned) {
        if (ConfigRegistry.RESTOCKER.getValue()) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (entity.level().isClientSide() && entity instanceof Player) {
                this.packtools$itemReturnedFromFinish = returned.copy();
            }
        }
        return returned;
    }

    @Inject(method = "completeUsingItem", at = @At("RETURN"))
    private void packtools$restockAfterConsume(CallbackInfo ci) {
        if (ConfigRegistry.RESTOCKER.getValue()) {
            LivingEntity entity = (LivingEntity) (Object) this;
            if (!entity.level().isClientSide() || !(entity instanceof Player player)) {
                return;
            }

            if (this.packtools$itemBeforeFinish != null) {

                ItemStack after = this.packtools$itemReturnedFromFinish != null
                        ? this.packtools$itemReturnedFromFinish
                        : ItemStack.EMPTY;

                ClientRestockerHelper.scheduleRestockChecked(
                        getUsedItemHand(),
                        player.getInventory(),
                        this.packtools$itemBeforeFinish,
                        after
                );

                ClientRestockerHelper.performRestock();
                this.packtools$itemBeforeFinish = null;
                this.packtools$itemReturnedFromFinish = null;
            }
        }
    }
}