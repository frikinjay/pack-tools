package com.frikinjay.packtools.util.tooltip;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.List;

/**
 * Tooltip component that holds potion effects data.
 */
public record EffectsTooltip(List<MobEffectInstance> effects) implements TooltipComponent {
}