package com.frikinjay.packtools.client.features.healthhud;

import com.frikinjay.packtools.PackTools;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only helper for rendering health HUD overflow overlay.
 * Renders colored hearts on top of vanilla health bar when health exceeds 20.
 */
public class ClientHealthHudHelper {

    private static final int ICON_WIDTH = 9;
    private static final int ICON_HEIGHT = 9;
    private static final int MAX_ICONS = 10;
    private static final int HEALTH_PER_ICON = 2;
    private static final int MAX_BASE_HEALTH = 20;

    /**
     * Renders the health overflow overlay on top of vanilla health bar.
     * Only renders when health exceeds 20 to show overflow colors.
     */
    public static void renderHealthOverlay(GuiGraphics guiGraphics, Player player, int leftX, int baseY, int currentHealth, int displayHealth, boolean blink) {
        if (player == null) return;

        if (currentHealth <= 20) return;

        boolean hasStatusEffect = player.hasEffect(MobEffects.POISON)
                || player.hasEffect(MobEffects.WITHER)
                || player.isFullyFrozen();

        if (hasStatusEffect) return;

        boolean hardcore = player.level().getLevelData().isHardcore();

        renderHealthIcons(guiGraphics, currentHealth, displayHealth, leftX, baseY, hardcore, blink);
    }

    /**
     * Renders health overlay icons with color cycling for overflow.
     * Always renders on first row only, overlaying vanilla hearts.
     */
    private static void renderHealthIcons(GuiGraphics guiGraphics, int currentHealth, int displayHealth, int leftX, int baseY, boolean hardcore, boolean blink) {
        int overflowLevel = currentHealth / MAX_BASE_HEALTH;
        int effectiveHealth = currentHealth % MAX_BASE_HEALTH;
        if (effectiveHealth == 0 && currentHealth > 0) {
            overflowLevel--;
            effectiveHealth = MAX_BASE_HEALTH;
        }

        if (overflowLevel == 0) return; // No overflow

        int colorIndex = (overflowLevel - 1) % 4;

        int fullHearts = effectiveHealth / HEALTH_PER_ICON;
        boolean hasHalf = (effectiveHealth % HEALTH_PER_ICON) == 1;

        int emptyColorIndex = overflowLevel > 1 ? (overflowLevel - 2) % 4 : -1;

        for (int i = 0; i < MAX_ICONS; i++) {
            int x = leftX + i * 8;
            int y = baseY;

            Identifier texture = null;

            if (i < fullHearts) {
                boolean shouldBlink = blink && (i * 2 + 1) <= displayHealth;
                texture = getFullHeartTexture(colorIndex, hardcore, shouldBlink);
            } else if (i == fullHearts && hasHalf) {
                boolean shouldBlink = blink && (i * 2 + 1) <= displayHealth;

                if (emptyColorIndex >= 0) {
                    Identifier backgroundTexture = getFullHeartTexture(emptyColorIndex, hardcore, false);
                    guiGraphics.blit(RenderPipelines.GUI_TEXTURED, backgroundTexture, x, y, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
                }

                texture = getHalfHeartTexture(colorIndex, hardcore, shouldBlink);
            } else if (emptyColorIndex >= 0) {

                texture = getFullHeartTexture(emptyColorIndex, hardcore, false);
            }

            if (texture != null) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
            }
        }
    }

    /**
     * Gets the appropriate full heart texture based on color, hardcore, and blink state.
     */
    private static Identifier getFullHeartTexture(int colorIndex, boolean hardcore, boolean blink) {
        String color = getColorName(colorIndex);
        String prefix = hardcore ? "hardcore_" : "";
        String suffix = blink ? "_blinking" : "";

        return Identifier.fromNamespaceAndPath(
                PackTools.MOD_ID,
                "textures/gui/hud/health/" + prefix + "full" + suffix + "_" + color + ".png"
        );
    }

    /**
     * Gets the appropriate half heart texture based on color, hardcore, and blink state.
     */
    private static Identifier getHalfHeartTexture(int colorIndex, boolean hardcore, boolean blink) {
        String color = getColorName(colorIndex);
        String prefix = hardcore ? "hardcore_" : "";
        String suffix = blink ? "_blinking" : "";

        return Identifier.fromNamespaceAndPath(
                PackTools.MOD_ID,
                "textures/gui/hud/health/" + prefix + "half" + suffix + "_" + color + ".png"
        );
    }

    /**
     * Gets the color name for the given overflow index.
     */
    private static String getColorName(int index) {
        return switch (index % 4) {
            case 0 -> "yellow";
            case 1 -> "orange";
            case 2 -> "purple";
            case 3 -> "blue";
            default -> "yellow";
        };
    }
}