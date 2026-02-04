package com.frikinjay.packtools.client.features.armorhud;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.util.HudPositionManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Client-only helper for rendering armor HUD with overflow.
 */
public class ClientArmorHudHelper {

    private static final int ICON_WIDTH = 9;
    private static final int ICON_HEIGHT = 9;
    private static final int ICON_SPACING = -1;
    private static final int MAX_ICONS = 10;
    private static final float ARMOR_PER_ICON = 2.0f;
    private static final float MAX_BASE_ARMOR = 20.0f;

    // Base textures (used for 0-20 armor)
    private static final Identifier EMPTY_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_empty.png");
    private static final Identifier HALF_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_half.png");
    private static final Identifier FULL_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_full.png");

    // Colored textures for overflow (20+ armor)
    private static final Identifier HALF_TEXTURE_YELLOW_OVERFLOW = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_half_yellow_overflow.png");

    private static final Identifier[] HALF_TEXTURES_COLORED = {
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_half_yellow.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_half_orange.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_half_purple.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_half_blue.png")
    };

    private static final Identifier[] FULL_TEXTURES_COLORED = {
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_full_yellow.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_full_orange.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_full_purple.png"),
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/hud/armor/armor_full_blue.png")
    };

    /**
     * Renders the armor HUD above the health bar using pre-calculated layout.
     */
    public static void renderArmorHud(GuiGraphics guiGraphics, HudPositionManager.HudLayout layout, Player player) {
        if (player == null) return;

        int armor = player.getArmorValue();
        if (armor <= 0) return;

        renderArmorIcons(guiGraphics, armor, layout.leftX, layout.armorY);
    }

    /**
     * Renders armor icons with color cycling for overflow.
     * Icons fill from LEFT to RIGHT.
     */
    private static void renderArmorIcons(GuiGraphics guiGraphics, int armor, int leftX, int y) {
        // Calculate overflow level (how many times we've exceeded 20 armor)
        int overflowLevel = armor / (int) MAX_BASE_ARMOR;
        int effectiveArmor = armor % (int) MAX_BASE_ARMOR;
        if (effectiveArmor == 0 && armor > 0) {
            overflowLevel--;
            effectiveArmor = (int) MAX_BASE_ARMOR;
        }

        // Calculate how many full and half icons to show (from the left)
        int fullIcons = (int) (effectiveArmor / ARMOR_PER_ICON);
        boolean hasHalf = (effectiveArmor % (int) ARMOR_PER_ICON) >= 1;

        // Determine if we should use colored sprites (overflow > 0 means armor > 20)
        boolean useColoredSprites = overflowLevel > 0;

        // Determine which color set to use based on overflow
        int colorIndex = (overflowLevel - 1) % 4;
        boolean isSecondCycle = overflowLevel > 4;

        // For empty icons when overflowed, use the PREVIOUS color cycle
        int emptyColorIndex = overflowLevel > 1 ? (overflowLevel - 2) % 4 : -1;

        // Render from left to right
        for (int i = 0; i < MAX_ICONS; i++) {
            int x = leftX + i * (ICON_WIDTH + ICON_SPACING);

            // Determine which texture to use (fill from left to right)
            Identifier texture;
            if (i < fullIcons) {
                texture = useColoredSprites ? FULL_TEXTURES_COLORED[colorIndex] : FULL_TEXTURE;
            } else if (i == fullIcons && hasHalf) {
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
     * Calculates the height needed for the armor HUD.
     */
    public static int getArmorHudHeight(Player player) {
        if (player == null) return 0;
        int armor = player.getArmorValue();
        if (armor <= 0) return 0;
        return ICON_HEIGHT + 1;
    }
}