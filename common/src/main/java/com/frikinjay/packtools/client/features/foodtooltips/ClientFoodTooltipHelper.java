package com.frikinjay.packtools.client.features.foodtooltips;

import com.frikinjay.packtools.client.util.ClientFoodTooltip;
import com.frikinjay.packtools.client.util.MultiTooltip;
import com.frikinjay.packtools.util.tooltip.FoodTooltip;
import com.frikinjay.packtools.util.tooltip.MultiTooltipData;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only helper - contains rendering calculations.
 */
public class ClientFoodTooltipHelper {

    private static final int ICON_SIZE = 9;
    private static final int SPACING = 2;

    public static ClientTooltipComponent createClientComponent(TooltipComponent data) {
        if (data instanceof FoodTooltip foodData) {
            return new ClientFoodTooltip(foodData);
        }
        if (data instanceof MultiTooltipData multi) {
            return createMultiComponent(multi);
        }
        return null;
    }

    private static ClientTooltipComponent createMultiComponent(MultiTooltipData multi) {
        List<ClientTooltipComponent> components = new ArrayList<>();

        for (TooltipComponent tooltip : multi.tooltips()) {
            components.add(ClientTooltipComponent.create(tooltip));
        }

        return new MultiTooltip(components);
    }

    public static int calculateWidth(FoodProperties food, Font font) {
        float hungerValue = food.nutrition() / 2.0f;
        String hungerText = formatValue(hungerValue);
        String saturationText = formatValue(food.saturation() / 2.0f);

        // Icon + spacing + "x" + hunger value + spacing + Icon + spacing + "x" + saturation value
        int width = ICON_SIZE + SPACING;  // Food icon
        width += font.width("x" + hungerText) + 4;  // Hunger text with extra spacing
        width += ICON_SIZE + SPACING;  // Saturation icon
        width += font.width("x" + saturationText);  // Saturation text

        return width;
    }

    private static String formatValue(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.1f", value);
        }
    }
}