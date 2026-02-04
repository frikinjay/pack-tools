package com.frikinjay.packtools.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders multiple tooltips vertically stacked (e.g., food + effects).
 */
public class MultiTooltip implements ClientTooltipComponent {
    private static final int SPACING = 2;
    private final List<ClientTooltipComponent> components;

    public MultiTooltip(List<ClientTooltipComponent> components) {
        this.components = components;
    }

    @Override
    public int getHeight(@NotNull Font font) {
        int totalHeight = 0;
        for (int i = 0; i < components.size(); i++) {
            totalHeight += components.get(i).getHeight(font);
            if (i < components.size() - 1) {
                totalHeight += SPACING;
            }
        }
        return totalHeight;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        int maxWidth = 0;
        for (ClientTooltipComponent component : components) {
            maxWidth = Math.max(maxWidth, component.getWidth(font));
        }
        return maxWidth;
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, int w, int h, @NotNull GuiGraphics guiGraphics) {
        int currentY = y;

        for (ClientTooltipComponent component : components) {
            int componentHeight = component.getHeight(font);
            component.renderImage(font, x, currentY, w, componentHeight, guiGraphics);
            currentY += componentHeight + SPACING;
        }
    }
}