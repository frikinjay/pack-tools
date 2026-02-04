package com.frikinjay.packtools.util.tooltip;

import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FoodTooltip(FoodProperties food) implements TooltipComponent {
}