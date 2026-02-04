package com.frikinjay.packtools.util.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/**
 * Enum for identifying different custom tooltip types for ordering.
 */
public enum TooltipType {
    VANILLA,
    FOOD,
    EFFECTS;

    /**
     * Gets the tooltip type from a TooltipComponent.
     */
    public static TooltipType getType(TooltipComponent tooltip) {
        if (tooltip instanceof FoodTooltip) {
            return FOOD;
        } else if (tooltip instanceof EffectsTooltip) {
            return EFFECTS;
        } else {
            return VANILLA;
        }
    }
}