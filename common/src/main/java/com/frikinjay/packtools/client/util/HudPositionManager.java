package com.frikinjay.packtools.client.util;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;

/**
 * Manages dynamic positioning for custom HUD elements to avoid overlapping with vanilla HUD elements.
 * All positions are calculated relative to vanilla HUD elements for maximum compatibility.
 */
public class HudPositionManager {

    // Vanilla HUD element sizes (from Gui.java)
    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 1; // Standard 1 pixel spacing between rows
    private static final int VEHICLE_HEART_ROW_HEIGHT = 10; // 9 pixels + 1 spacing

    /**
     * Calculates the complete HUD layout in one pass for all custom elements.
     * This ensures consistent positioning across all HUD elements.
     *
     * @param toughnessHeight The height of the toughness HUD (must be pre-calculated)
     * @param effectsHeight The height of the effects HUD (must be pre-calculated)
     */
    public static HudLayout calculateLayout(int screenWidth, int screenHeight, Player player,
                                            int toughnessHeight, int effectsHeight) {
        if (player == null) {
            return new HudLayout(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        // Base positions - vanilla uses screenHeight - 39 for the main HUD row
        int baseHudY = screenHeight - 39;
        int leftX = screenWidth / 2 - 91;
        int rightX = screenWidth / 2 + 91;

        // DETECT which vanilla elements are visible and their heights
        LivingEntity vehicle = getPlayerVehicleWithHealth(player);
        int vehicleHealthRows = getVehicleHealthRows(vehicle);
        boolean hasVehicleHealth = vehicleHealthRows > 0;

        // Air bubbles are only shown when NOT mounted (vanilla behavior from Gui.renderPlayerHealth)
        // When mounted, vehicle health replaces the food bar, and air bubbles are not shown
        boolean hasAirBubbles = !hasVehicleHealth && shouldShowAirBubbles(player);
        int airBubbleHeight = hasAirBubbles ? (ICON_SIZE + ICON_SPACING) : 0;

        // Vehicle health calculation
        // CRITICAL: Vehicle health REPLACES food bar, doesn't stack above it
        // Only additional rows beyond the first add to the offset
        int vehicleHealthExtraRows = Math.max(0, vehicleHealthRows - 1);
        int vehicleHealthHeight = vehicleHealthExtraRows * VEHICLE_HEART_ROW_HEIGHT;

        // LEFT SIDE (Health/Armor side)
        // Armor always sits exactly one row above the health bar
        int armorY = baseHudY - ICON_SIZE - ICON_SPACING;

        // RIGHT SIDE (Food/Air/Vehicle side)
        // Vanilla behavior:
        // - Food bar at base position (always there when not mounted)
        // - Air bubbles above food (only when not mounted AND (underwater OR air depleted))
        // - Vehicle health REPLACES food bar (first row at base, additional rows stack up)
        // - Our custom elements (toughness, effects) go above whatever is visible

        // Toughness position calculation:
        // - If not mounted: baseY - airBubbles - spacing
        // - If mounted with 1 row (≤10 hearts): baseY - spacing (vehicle replaces food at base)
        // - If mounted with 2+ rows: baseY - extraRows - spacing
        int toughnessY = baseHudY - airBubbleHeight - vehicleHealthHeight - ICON_SIZE - ICON_SPACING;

        // Effects position (sits above toughness)
        int effectsBottomY = baseHudY - airBubbleHeight - vehicleHealthHeight - toughnessHeight - ICON_SIZE - ICON_SPACING;

        return new HudLayout(
                leftX,
                rightX,
                baseHudY,
                armorY,
                toughnessY,
                effectsBottomY,
                airBubbleHeight,
                vehicleHealthHeight,
                vehicleHealthRows,
                toughnessHeight,
                effectsHeight
        );
    }

    /**
     * Determines if air bubbles should be shown.
     * From Gui.renderAirBubbles() - shown when underwater OR when air < max
     */
    private static boolean shouldShowAirBubbles(Player player) {
        int maxAir = player.getMaxAirSupply();
        int currentAir = player.getAirSupply();
        boolean isUnderwater = player.isEyeInFluid(FluidTags.WATER);

        // Vanilla logic: show if underwater OR air is depleted
        return isUnderwater || currentAir < maxAir;
    }

    /**
     * Gets the vehicle entity if it has health to display.
     * From Gui.getPlayerVehicleWithHealth()
     */
    private static LivingEntity getPlayerVehicleWithHealth(Player player) {
        if (player == null) {
            return null;
        }

        var vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity livingEntity) {
            return livingEntity;
        }

        return null;
    }

    /**
     * Calculates number of rows needed for vehicle health display.
     * From Gui.renderVehicleHealth() and Gui.getVehicleMaxHearts()
     */
    private static int getVehicleHealthRows(LivingEntity vehicle) {
        if (vehicle == null || !vehicle.showVehicleHealth()) {
            return 0;
        }

        float maxHealth = vehicle.getMaxHealth();
        int hearts = (int) (maxHealth + 0.5F) / 2;

        // Cap at 30 hearts (vanilla behavior)
        hearts = Math.min(hearts, 30);

        if (hearts == 0) {
            return 0;
        }

        // Calculate rows: vanilla uses Math.ceil(hearts / 10.0)
        return (int) Math.ceil((double) hearts / 10.0);
    }

    /**
     * Gets toughness HUD height.
     * This must be calculated based on player's toughness attribute.
     */
    private static int getToughnessHudHeight(Player player) {
        int toughness = Mth.floor(player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS));
        if (toughness <= 0) {
            return 0;
        }

        return ICON_SIZE + ICON_SPACING;
    }

    /**
     * Calculates effects HUD height based on number of active effects.
     * Must account for effects wrapping to multiple rows.
     */
    public static int calculateEffectsHeight(int effectCount, int iconsPerRow) {
        if (effectCount == 0) {
            return 0;
        }

        int rows = (int) Math.ceil((double) effectCount / iconsPerRow);
        // Each row is ICON_SIZE tall, with ROW_SPACING between rows
        // The +2 at the end accounts for additional spacing
        return rows * (ICON_SIZE + ICON_SPACING) + 2;
    }

    /**
     * Complete HUD layout data structure containing all calculated positions.
     */
    public static class HudLayout {
        public final int leftX;
        public final int rightX;
        public final int baseHudY;

        // Custom element positions
        public final int armorY;
        public final int toughnessY;
        public final int effectsBottomY;

        // Element heights (for debugging/reference)
        public final int airBubbleHeight;        // Vanilla element
        public final int vehicleHealthHeight;    // Vanilla element
        public final int vehicleHealthRows;      // Vanilla element
        public final int toughnessHeight;        // Custom element
        public final int effectsHeight;          // Custom element

        private HudLayout(int leftX, int rightX, int baseHudY, int armorY, int toughnessY,
                          int effectsBottomY, int airBubbleHeight, int vehicleHealthHeight,
                          int vehicleHealthRows, int toughnessHeight, int effectsHeight) {
            this.leftX = leftX;
            this.rightX = rightX;
            this.baseHudY = baseHudY;
            this.armorY = armorY;
            this.toughnessY = toughnessY;
            this.effectsBottomY = effectsBottomY;
            this.airBubbleHeight = airBubbleHeight;
            this.vehicleHealthHeight = vehicleHealthHeight;
            this.vehicleHealthRows = vehicleHealthRows;
            this.toughnessHeight = toughnessHeight;
            this.effectsHeight = effectsHeight;
        }

        /**
         * Returns debug information about the current HUD layout.
         */
        public String getDebugInfo() {
            return String.format(
                    "HudLayout[leftX=%d, rightX=%d, baseY=%d, armorY=%d, toughnessY=%d, effectsY=%d, " +
                            "airH=%d, vehicleH=%d(rows=%d), toughnessH=%d, effectsH=%d]",
                    leftX, rightX, baseHudY, armorY, toughnessY, effectsBottomY,
                    airBubbleHeight, vehicleHealthHeight, vehicleHealthRows, toughnessHeight, effectsHeight
            );
        }
    }

    /**
     * Calculates Y position accounting for effects HUD height.
     * Use this when you need to position something above the effects HUD.
     */
    public static int getYPositionAboveEffects(HudLayout layout, int effectsHudHeight) {
        return layout.effectsBottomY - effectsHudHeight;
    }

    /**
     * Checks if vehicle health is currently visible.
     */
    public static boolean isVehicleHealthVisible(Player player) {
        return getVehicleHealthRows(getPlayerVehicleWithHealth(player)) > 0;
    }

    /**
     * Checks if air bubbles are currently visible.
     */
    public static boolean areAirBubblesVisible(Player player) {
        return shouldShowAirBubbles(player);
    }

    /**
     * Gets the total vertical offset from the base HUD position for right-side elements.
     * This is useful for calculating cumulative offsets.
     */
    public static int getRightSideVerticalOffset(Player player) {
        boolean hasAirBubbles = shouldShowAirBubbles(player);
        int airHeight = hasAirBubbles ? (ICON_SIZE + ICON_SPACING) : 0;

        LivingEntity vehicle = getPlayerVehicleWithHealth(player);
        int vehicleRows = getVehicleHealthRows(vehicle);
        int vehicleHeight = vehicleRows * VEHICLE_HEART_ROW_HEIGHT;

        return airHeight + vehicleHeight;
    }

    /**
     * Gets the base Y position for the main HUD row.
     */
    public static int getBaseHudY(int screenHeight) {
        return screenHeight - 39;
    }

    /**
     * Gets the left X position for left-aligned HUD elements.
     */
    public static int getLeftHudX(int screenWidth) {
        return screenWidth / 2 - 91;
    }

    /**
     * Gets the right X position for right-aligned HUD elements.
     */
    public static int getRightHudX(int screenWidth) {
        return screenWidth / 2 + 91;
    }
}