package com.frikinjay.packtools.features.effectstooltips;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.util.tooltip.EffectsTooltip;
import com.frikinjay.packtools.util.tooltip.MultiTooltipData;
import com.frikinjay.packtools.util.tooltip.TooltipOrdering;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared logic for effects tooltips - safe for both client and server.
 */
public class EffectsTooltipHelper {

    public static List<MobEffectInstance> extractEffects(ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        Consumable consumable = stack.get(DataComponents.CONSUMABLE);

        if (consumable == null) {
            return effects;
        }

        consumable.onConsumeEffects().forEach(consumeEffect -> {
            if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect applyEffect) {
                effects.addAll(applyEffect.effects());
            }
        });

        return effects;
    }

    public static Optional<TooltipComponent> injectEffectsTooltip(ItemStack stack, Optional<TooltipComponent> existing) {
        if (!ConfigRegistry.EFFECTS_TOOLTIPS.getValue()) {
            return existing;
        }

        List<MobEffectInstance> effects = extractEffects(stack);
        if (effects.isEmpty()) {
            return existing;
        }

        EffectsTooltip effectsTooltip = new EffectsTooltip(effects);

        if (existing.isPresent()) {
            TooltipComponent existingComponent = existing.get();
            List<TooltipComponent> tooltips = new ArrayList<>();

            // If it's already a MultiTooltipData, extract all tooltips
            if (existingComponent instanceof MultiTooltipData multi) {
                tooltips.addAll(multi.tooltips());
            } else {
                tooltips.add(existingComponent);
            }

            tooltips.add(effectsTooltip);

            // Order tooltips according to configured order
            tooltips = TooltipOrdering.orderTooltips(tooltips, PackTools.tooltipOrder);

            return Optional.of(new MultiTooltipData(tooltips));
        } else {
            return Optional.of(effectsTooltip);
        }
    }
}