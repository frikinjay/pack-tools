package com.frikinjay.packtools.client.features.effectstooltips;

import com.frikinjay.packtools.client.util.ClientEffectsTooltip;
import com.frikinjay.packtools.client.util.MultiTooltip;
import com.frikinjay.packtools.util.tooltip.EffectsTooltip;
import com.frikinjay.packtools.util.tooltip.MultiTooltipData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only helper for effects tooltips.
 */
public class ClientEffectsTooltipHelper {

    public static ClientTooltipComponent createClientComponent(TooltipComponent data) {
        if (data instanceof EffectsTooltip effectsData) {
            return new ClientEffectsTooltip(effectsData);
        }
        if (data instanceof MultiTooltipData multi) {
            return createMultiComponent(multi);
        }
        return null;
    }

    private static ClientTooltipComponent createMultiComponent(MultiTooltipData multi) {
        List<ClientTooltipComponent> components = new ArrayList<>();

        for (TooltipComponent tooltip : multi.tooltips()) {
            if (tooltip instanceof EffectsTooltip effectsData) {
                components.add(new ClientEffectsTooltip(effectsData));
            } else {
                components.add(ClientTooltipComponent.create(tooltip));
            }
        }

        return new MultiTooltip(components);
    }

    public static int calculateEffectWidth(MobEffectInstance effect, Font font) {
        String effectName = effect.getEffect().value().getDisplayName().getString();

        if (effect.getAmplifier() > 0) {
            int amplifier = effect.getAmplifier() + 1;
            String romanNumeral = toRomanNumeral(amplifier);
            effectName = effectName + " " + romanNumeral;
        }

        String duration = formatDuration(effect.getDuration());
        String fullText = effectName + " (" + duration + ")";

        return 9 + 2 + font.width(fullText);
    }

    private static String toRomanNumeral(int num) {
        return switch (num) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(num);
        };
    }

    public static int calculateMaxWidth(List<MobEffectInstance> effects, Font font) {
        int maxWidth = 0;
        for (MobEffectInstance effect : effects) {
            maxWidth = Math.max(maxWidth, calculateEffectWidth(effect, font));
        }
        return maxWidth;
    }

    private static String formatDuration(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
}