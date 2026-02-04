package com.frikinjay.packtools.util.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for ordering tooltips based on configured order.
 */
public class TooltipOrdering {

    /**
     * Sorts tooltips according to the order array.
     * Tooltips not in the order array are placed at the end in their original order.
     */
    public static List<TooltipComponent> orderTooltips(List<TooltipComponent> tooltips, TooltipType[] order) {
        if (tooltips.size() <= 1 || order.length == 0) {
            return tooltips;
        }

        // Create a map of type to priority
        Map<TooltipType, Integer> priorityMap = new HashMap<>();
        for (int i = 0; i < order.length; i++) {
            priorityMap.put(order[i], i);
        }

        // Create a copy to sort
        List<TooltipComponent> sorted = new ArrayList<>(tooltips);

        // Sort using the priority map
        sorted.sort((a, b) -> {
            TooltipType typeA = TooltipType.getType(a);
            TooltipType typeB = TooltipType.getType(b);

            int priorityA = priorityMap.getOrDefault(typeA, Integer.MAX_VALUE);
            int priorityB = priorityMap.getOrDefault(typeB, Integer.MAX_VALUE);

            return Integer.compare(priorityA, priorityB);
        });

        return sorted;
    }
}