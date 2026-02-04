package com.frikinjay.packtools.features.foodtooltips;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.util.tooltip.FoodTooltip;
import com.frikinjay.packtools.util.tooltip.MultiTooltipData;
import com.frikinjay.packtools.util.tooltip.TooltipOrdering;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared logic - safe for both client and server.
 */
public class FoodTooltipHelper {

    public static Optional<TooltipComponent> injectFoodTooltip(ItemStack stack, Optional<TooltipComponent> existing) {
        if (!ConfigRegistry.FOOD_TOOLTIPS.getValue()) {
            return existing;
        }

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return existing;
        }

        FoodTooltip foodTooltip = new FoodTooltip(food);

        if (existing.isPresent()) {
            List<TooltipComponent> tooltips = new ArrayList<>();
            tooltips.add(existing.get());
            tooltips.add(foodTooltip);

            // Order tooltips according to configured order
            tooltips = TooltipOrdering.orderTooltips(tooltips, PackTools.tooltipOrder);

            return Optional.of(new MultiTooltipData(tooltips));
        } else {
            return Optional.of(foodTooltip);
        }
    }
}