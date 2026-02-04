package com.frikinjay.packtools.client.features.effectshud;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.util.HudPositionManager;
import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.effectshud.EffectsHudHelper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Client-only helper for rendering effects HUD with dynamic positioning.
 */
public class ClientEffectsHudHelper {

    private static final Identifier POTION_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/potion.png");
    private static final Identifier POTION_BG_TEXTURE = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "textures/gui/potion_bg.png");
    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = -1;
    private static final int ROW_SPACING = 1;
    private static final int ICONS_PER_ROW = 10;

    /**
     * Renders the effects HUD using pre-calculated layout.
     * Position automatically adjusts based on air bubbles, vehicle health, and toughness visibility.
     */
    public static void renderEffectsHud(GuiGraphics guiGraphics, HudPositionManager.HudLayout layout, Player player) {
        if (!ConfigRegistry.EFFECTS_HUD.getValue()) return;

        List<MobEffectInstance> effects = EffectsHudHelper.getActiveEffects(player);
        if (effects.isEmpty()) return;

        renderEffectIcons(guiGraphics, effects, layout.rightX, layout.effectsBottomY);
    }

    /**
     * Renders effect icons, wrapping to multiple rows if needed.
     */
    private static void renderEffectIcons(GuiGraphics guiGraphics, List<MobEffectInstance> effects, int rightX, int bottomY) {
        int total = effects.size();

        for (int i = 0; i < total; i++) {
            // Reverse order: newest (last in list) appears rightmost
            MobEffectInstance effect = effects.get(total - 1 - i);

            int row = i / ICONS_PER_ROW;
            int col = i % ICONS_PER_ROW;

            // Calculate how many icons are in this row
            int iconsInRow = Math.min(ICONS_PER_ROW, total - (row * ICONS_PER_ROW));
            int rowWidth = iconsInRow * ICON_SIZE + (iconsInRow - 1) * ICON_SPACING;

            // Position: right-aligned, stacking upward
            int x = rightX - rowWidth + col * (ICON_SIZE + ICON_SPACING);
            int y = bottomY - row * (ICON_SIZE + ROW_SPACING);

            renderEffectIcon(guiGraphics, effect, x, y);
        }
    }

    /**
     * Renders a single effect icon with colored background.
     */
    private static void renderEffectIcon(GuiGraphics guiGraphics, MobEffectInstance effect, int x, int y) {
        int color = effect.getEffect().value().getColor();

        // Colored fill behind potion bottle
        guiGraphics.fill(x + 2, y + 3, x + ICON_SIZE - 2, y + ICON_SIZE, ARGB.opaque(color));

        // Potion bottle layers
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, POTION_BG_TEXTURE, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, POTION_TEXTURE, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }

    /**
     * Calculates the total height needed for the effects HUD.
     */
    public static int getEffectsHudHeight(Player player) {
        if (!ConfigRegistry.EFFECTS_HUD.getValue()) return 0;

        List<MobEffectInstance> effects = EffectsHudHelper.getActiveEffects(player);
        if (effects.isEmpty()) return 0;

        return HudPositionManager.calculateEffectsHeight(effects.size(), ICONS_PER_ROW);
    }
}