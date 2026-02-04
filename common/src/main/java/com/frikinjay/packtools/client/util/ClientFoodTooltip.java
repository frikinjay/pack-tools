package com.frikinjay.packtools.client.util;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.features.foodtooltips.ClientFoodTooltipHelper;
import com.frikinjay.packtools.util.tooltip.FoodTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.food.FoodProperties;
import org.jetbrains.annotations.NotNull;

public class ClientFoodTooltip implements ClientTooltipComponent {

    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier SATURATION_FULL = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/saturation.png");
    private static final int ICON_SIZE = 9;
    private static final int SPACING = 2;
    private static final int GOLD_COLOR = 0xFFFFAA00;

    public final FoodProperties food;

    public ClientFoodTooltip(FoodTooltip data) {
        this.food = data.food();
    }

    @Override
    public int getHeight(@NotNull Font font) {
        return 10;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return ClientFoodTooltipHelper.calculateWidth(food, font);
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, int w, int h, @NotNull GuiGraphics guiGraphics) {
        int currentX = x;

        // Render food shank icon
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOOD_FULL, currentX, y - 1, ICON_SIZE, ICON_SIZE);
        currentX += ICON_SIZE + SPACING;

        // Render hunger value
        float hungerValue = food.nutrition() / 2.0f;
        String hungerText = formatValue(hungerValue);
        guiGraphics.drawString(font, "x" + hungerText, currentX, y, 0xFFFFFFFF);
        currentX += font.width("x" + hungerText) + 4;

        // Render saturation icon
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SATURATION_FULL, currentX, y - 1, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        currentX += ICON_SIZE + SPACING;

        // Render saturation value in gold
        String saturationText = formatValue(food.saturation() / 2.0f);
        guiGraphics.drawString(font, "x" + saturationText, currentX, y, GOLD_COLOR);
    }

    private String formatValue(float value) {
        if (value == (int) value) {
            return String.valueOf((int) value);
        } else {
            return String.format("%.1f", value);
        }
    }
}