package com.frikinjay.packtools.mixin.features.stackmaxxer;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.stackmaxxer.StackMaxxerHelper;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @ModifyReturnValue(method = "getMaxStackSize", at = @At("RETURN"))
    private int packtools$modifyStackSize(int original) {
        if (ConfigRegistry.STACK_MAXXER.getValue()) {
            ItemStack instance = (ItemStack) (Object) this;
            return StackMaxxerHelper.getModifiedStackSize(instance.getItem(), original);
        }
        return original;
    }
}