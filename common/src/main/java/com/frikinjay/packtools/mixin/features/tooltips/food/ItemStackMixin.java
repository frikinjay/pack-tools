package com.frikinjay.packtools.mixin.features.tooltips.food;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.effectstooltips.EffectsTooltipHelper;
import com.frikinjay.packtools.features.foodtooltips.FoodTooltipHelper;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void packtools$injectCustomTooltips(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        if (ConfigRegistry.FOOD_TOOLTIPS.getValue()) {
            ItemStack self = (ItemStack) (Object) this;
            // First inject food tooltip
            Optional<TooltipComponent> result = FoodTooltipHelper.injectFoodTooltip(self, cir.getReturnValue());
            // Then inject effects tooltip (combine em)
            result = EffectsTooltipHelper.injectEffectsTooltip(self, result);
            cir.setReturnValue(result);
        }
    }
}