package com.frikinjay.packtools.client.features.toughnesshud;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.util.HudPositionManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only helper for rendering toughness HUD with dynamic positioning.
 */
public class ClientToughnessHudHelper {

    private static final int ICON_WIDTH = 9;
    private static final int ICON_HEIGHT = 9;
    private static final int ICON_SPACING = -1;
    private static final int MAX_ICONS = 10;
    private static final float TOUGHNESS_PER_ICON = 2.0f;
    private static final float MAX_BASE_TOUGHNESS = 20.0f;

    // Base textures (used for 0-20 toughness)
    private static final Identifier EMPTY_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_empty.png");
    private static final Identifier HALF_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_half.png");
    private static final Identifier FULL_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_full.png");

    // Colored textures for overflow (20+ toughness)
    private static final Identifier HALF_TEXTURE_YELLOW_OVERFLOW = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_half_yellow_overflow.png");

    private static final Identifier[] HALF_TEXTURES_COLORED = {
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_half_yellow.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_half_orange.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_half_purple.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_half_blue.png")
    };

    private static final Identifier[] FULL_TEXTURES_COLORED = {
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_full_yellow.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_full_orange.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_full_purple.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/toughness/toughness_full_blue.png")
    };

    /**
     * Renders the toughness HUD using pre-calculated layout.
     * Position automatically adjusts based on air bubbles and vehicle health visibility.
     */
    public static void renderToughnessHud(GuiGraphics guiGraphics, HudPositionManager.HudLayout layout, Player player) {
        if (player == null) return;

        int toughness = net.minecraft.util.Mth.floor(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (toughness <= 0) return;

        // Right X is shifted left by 1 pixel for proper alignment
        int rightX = layout.rightX - 1;

        renderToughnessIcons(guiGraphics, toughness, rightX, layout.toughnessY);
    }

    /**
     * Renders toughness icons with color cycling for overflow.
     * Icons fill from RIGHT to LEFT.
     */
    private static void renderToughnessIcons(GuiGraphics guiGraphics, int toughness, int rightX, int y) {
        // Calculate overflow level
        int overflowLevel = toughness / (int) MAX_BASE_TOUGHNESS;
        int effectiveToughness = toughness % (int) MAX_BASE_TOUGHNESS;
        if (effectiveToughness == 0 && toughness > 0) {
            overflowLevel--;
            effectiveToughness = (int) MAX_BASE_TOUGHNESS;
        }

        // Calculate how many full and half icons to show
        int fullIcons = (int) (effectiveToughness / TOUGHNESS_PER_ICON);
        boolean hasHalf = (effectiveToughness % (int) TOUGHNESS_PER_ICON) >= 1;

        // Determine if we should use colored sprites
        boolean useColoredSprites = overflowLevel > 0;

        // Determine which color set to use
        int colorIndex = (overflowLevel - 1) % 4;
        boolean isSecondCycle = overflowLevel > 4;

        // For empty icons when overflowed, use the PREVIOUS color cycle
        int emptyColorIndex = overflowLevel > 1 ? (overflowLevel - 2) % 4 : -1;

        // Render from left to right (visual position), but fill from right
        for (int i = 0; i < MAX_ICONS; i++) {
            int x = rightX - (MAX_ICONS - i) * (ICON_WIDTH + ICON_SPACING);

            // Calculate which icon position from the right this is
            int positionFromRight = MAX_ICONS - 1 - i;

            // Determine which texture to use (fill from right to left)
            Identifier texture;
            if (positionFromRight < fullIcons) {
                texture = useColoredSprites ? FULL_TEXTURES_COLORED[colorIndex] : FULL_TEXTURE;
            } else if (positionFromRight == fullIcons && hasHalf) {
                if (useColoredSprites) {
                    texture = (isSecondCycle && colorIndex == 0) ? HALF_TEXTURE_YELLOW_OVERFLOW : HALF_TEXTURES_COLORED[colorIndex];
                } else {
                    texture = HALF_TEXTURE;
                }
            } else {
                if (useColoredSprites && emptyColorIndex >= 0) {
                    texture = FULL_TEXTURES_COLORED[emptyColorIndex];
                } else if (useColoredSprites && emptyColorIndex < 0) {
                    texture = FULL_TEXTURE;
                } else {
                    texture = EMPTY_TEXTURE;
                }
            }

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0, 0, ICON_WIDTH, ICON_HEIGHT, ICON_WIDTH, ICON_HEIGHT);
        }
    }

    /**
     * Calculates the height needed for the toughness HUD.
     */
    public static int getToughnessHudHeight(Player player) {
        if (player == null) return 0;
        int toughness = net.minecraft.util.Mth.floor(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        if (toughness <= 0) return 0;
        return ICON_HEIGHT + 1;
    }
}