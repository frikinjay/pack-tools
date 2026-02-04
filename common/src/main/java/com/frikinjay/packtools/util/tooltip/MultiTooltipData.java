package com.frikinjay.packtools.util.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/**
 * Holds multiple custom tooltips together (e.g., food + effects).
 */
public record MultiTooltipData(List<TooltipComponent> tooltips) implements TooltipComponent {
}