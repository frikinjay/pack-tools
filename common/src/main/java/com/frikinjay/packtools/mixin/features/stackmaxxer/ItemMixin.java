package com.frikinjay.packtools.mixin.features.stackmaxxer;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.stackmaxxer.StackMaxxerHelper;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public abstract class ItemMixin {

    @ModifyReturnValue(method = "getDefaultMaxStackSize", at = @At("RETURN"))
    private int packtools$modifyDefaultStackSize(int original) {
        if (ConfigRegistry.STACK_MAXXER.getValue()) {
            return StackMaxxerHelper.getModifiedStackSize((Item) (Object) this, original);
        }
        return original;
    }
}