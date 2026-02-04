package com.frikinjay.packtools.client.util;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.features.effectstooltips.ClientEffectsTooltipHelper;
import com.frikinjay.packtools.util.tooltip.EffectsTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Renders potion effects in tooltips.
 * Each effect shows as: <tinted potion icon> <effect name> (duration)
 */
public class ClientEffectsTooltip implements ClientTooltipComponent {

    private static final Identifier POTION_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/potion.png");
    private static final int ICON_SIZE = 9;
    private static final int LINE_HEIGHT = 10;
    private static final int ICON_TEXT_PADDING = 2;

    private final List<MobEffectInstance> effects;

    public ClientEffectsTooltip(EffectsTooltip data) {
        this.effects = data.effects();
    }

    @Override
    public int getHeight(@NotNull Font font) {
        return effects.size() * LINE_HEIGHT;
    }

    @Override
    public int getWidth(@NotNull Font font) {
        return ClientEffectsTooltipHelper.calculateMaxWidth(effects, font);
    }

    @Override
    public void renderImage(@NotNull Font font, int x, int y, int w, int h, @NotNull GuiGraphics guiGraphics) {
        int currentY = y;

        for (MobEffectInstance effect : effects) {
            // Get the potion color for the background
            int effectColor = effect.getEffect().value().getColor();

            // Render colored background (simulating filled potion bottle)
            renderColoredBackground(guiGraphics, x, currentY, effectColor);

            // Render white potion bottle icon on top
            renderPotionIcon(guiGraphics, x, currentY);

            // Format and render effect text
            renderEffectText(guiGraphics, font, effect, x + ICON_SIZE + ICON_TEXT_PADDING, currentY);

            currentY += LINE_HEIGHT;
        }
    }

    /**
     * Renders the colored background behind the potion icon (simulating potion liquid).
     */
    private void renderColoredBackground(GuiGraphics guiGraphics, int x, int y, int color) {
        // Convert color to ARGB format with full opacity
        int argbColor = ARGB.opaque(color);

        // Draw a filled rectangle as the potion liquid background
        // Make it slightly smaller than the icon to create a border effect
        guiGraphics.fill(x + 2, y + 3, x + ICON_SIZE - 2, y + ICON_SIZE - 2, argbColor);
    }

    /**
     * Renders the potion bottle icon using blit (for regular PNG textures).
     */
    private void renderPotionIcon(GuiGraphics guiGraphics, int x, int y) {
        // Use blit() for regular PNG textures (not sprites)
        // Parameters: renderPipeline, texture, x, y, u, v, width, height, textureWidth, textureHeight
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, POTION_TEXTURE, x, y-1, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    /**
     * Renders the effect text with proper formatting and color.
     */
    private void renderEffectText(GuiGraphics guiGraphics, Font font, MobEffectInstance effect, int x, int y) {
        String effectText = formatEffectText(effect);
        int textColor = getEffectTextColor(effect);

        // Draw string
        guiGraphics.drawString(font, effectText, x, y, textColor, true);
    }

    /**
     * Formats effect text as: <effect name> (<duration>)
     */
    private String formatEffectText(MobEffectInstance effect) {
        String name = effect.getEffect().value().getDisplayName().getString();
        String duration = formatDuration(effect.getDuration());

        // Add amplifier if greater than 0 (e.g., Speed II)
        if (effect.getAmplifier() > 0) {
            int amplifier = effect.getAmplifier() + 1;
            name = name + " " + toRomanNumeral(amplifier);
        }

        return name + " (" + duration + ")";
    }

    /**
     * Converts number to Roman numeral (I-X).
     */
    private String toRomanNumeral(int num) {
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

    /**
     * Formats duration as minutes:seconds (e.g., "1:30", "0:45")
     */
    private String formatDuration(int ticks) {
        int seconds = ticks / 20; // 20 ticks = 1 second
        int minutes = seconds / 60;
        int remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }

    /**
     * Gets the text color for effect based on whether it's beneficial or harmful.
     */
    private int getEffectTextColor(MobEffectInstance effect) {
        MobEffectCategory category = effect.getEffect().value().getCategory();
        return switch (category) {
            case BENEFICIAL -> 0xFF5555FF; // Blue with full alpha
            case HARMFUL -> 0xFFFF5555; // Red with full alpha
            case NEUTRAL -> 0xFFAAAAAA; // Gray with full alpha
        };
    }
}