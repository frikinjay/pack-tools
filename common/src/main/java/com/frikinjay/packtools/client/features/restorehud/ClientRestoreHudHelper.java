package com.frikinjay.packtools.client.features.restorehud;

import com.frikinjay.packtools.PackTools;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Client-only helper for rendering health and food restoration overlay.
 * Shows pulsing hearts/food shanks that will be gained when consuming held items.
 */
public class ClientRestoreHudHelper {

    private static final int ICON_SIZE = 9;
    private static final int MAX_ICONS = 10;
    private static final int HEALTH_PER_ICON = 2;
    private static final int FOOD_PER_ICON = 2;
    private static final int MAX_BASE_HEALTH = 20;

    // Vanilla food textures
    private static final Identifier FOOD_FULL = Identifier.withDefaultNamespace("hud/food_full");
    private static final Identifier FOOD_HALF = Identifier.withDefaultNamespace("hud/food_half");
    private static final Identifier FOOD_FULL_HUNGER = Identifier.withDefaultNamespace("hud/food_full_hunger");
    private static final Identifier FOOD_HALF_HUNGER = Identifier.withDefaultNamespace("hud/food_half_hunger");

    /**
     * Renders health restoration overlay showing how much health will be restored.
     * Only renders when player is holding a consumable that restores health and health is not full.
     */
    public static void renderHealthRestoreOverlay(GuiGraphics guiGraphics, Player player, int leftX, int baseY, int tickCount, RandomSource random, float tickDelta) {
        if (player == null) return;

        // Get held item and check if it's consumable
        ItemStack heldItem = getConsumableItem(player);
        if (heldItem.isEmpty()) return;

        // Get food properties to calculate health restoration
        FoodProperties foodProps = heldItem.get(DataComponents.FOOD);
        if (foodProps == null) return;

        FoodData foodData = player.getFoodData();
        int currentFood = foodData.getFoodLevel();
        float currentSaturation = foodData.getSaturationLevel();

        // Calculate what food level will be after eating
        int foodRestore = foodProps.nutrition();
        int newFoodLevel = Math.min(20, currentFood + foodRestore);

        // Calculate saturation after eating
        float saturationGain = foodRestore * foodProps.saturation();
        float newSaturation = Math.min(newFoodLevel, currentSaturation + saturationGain);

        // Calculate health that will be restored based on the new food/saturation levels
        float healthRestore = calculateHealthRegeneration(currentFood, newFoodLevel, currentSaturation, newSaturation);

        if (healthRestore <= 0) return;

        // Only render if player's health is not full
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth >= maxHealth) return;

        // Check for status effects - if present, don't overlay colored hearts
        boolean hasStatusEffect = player.hasEffect(MobEffects.POISON)
                || player.hasEffect(MobEffects.WITHER)
                || player.isFullyFrozen();

        if (hasStatusEffect) return; // Skip restore overlay when status effects are active

        // Calculate how much health will actually be restored (capped at max)
        float effectiveRestore = Math.min(healthRestore, maxHealth - currentHealth);

        // Calculate pulsing alpha for animation (keep original implementation)
        float alpha = getPulsingAlpha(tickDelta);

        boolean hardcore = player.level().getLevelData().isHardcore();

        renderHealthRestoreIcons(guiGraphics, player, currentHealth, effectiveRestore, leftX, baseY, tickCount, random, alpha, hardcore);
    }

    /**
     * Renders food restoration overlay showing how much food will be restored.
     * Only renders when player is holding a consumable food and food level is not full.
     */
    public static void renderFoodRestoreOverlay(GuiGraphics guiGraphics, Player player, int rightX, int baseY, int tickCount, RandomSource random, float tickDelta) {
        if (player == null) return;

        // Get held item and check if it's consumable
        ItemStack heldItem = getConsumableItem(player);
        if (heldItem.isEmpty()) return;

        // Get food properties
        FoodProperties foodProps = heldItem.get(DataComponents.FOOD);
        if (foodProps == null) return;

        int foodRestore = foodProps.nutrition();
        if (foodRestore <= 0) return;

        // Only render if player's food level is not full
        FoodData foodData = player.getFoodData();
        int currentFood = foodData.getFoodLevel();
        if (currentFood >= 20) return;

        // Calculate how much food will actually be restored (capped at 20)
        int effectiveRestore = Math.min(foodRestore, 20 - currentFood);

        // Calculate pulsing alpha for animation
        float alpha = getPulsingAlpha(tickDelta);

        // Check if player has hunger effect
        boolean hasHunger = player.hasEffect(MobEffects.HUNGER);

        renderFoodRestoreIcons(guiGraphics, player, currentFood, effectiveRestore, rightX, baseY, tickCount, random, alpha, hasHunger);
    }

    /**
     * Gets the consumable item from main hand or offhand.
     */
    private static ItemStack getConsumableItem(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.has(DataComponents.CONSUMABLE)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (offHand.has(DataComponents.CONSUMABLE)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static float calculateHealthRegeneration(int currentFood, int newFood, float currentSaturation, float newSaturation) {
        float totalHealthRestore = 0.0f;

        float food = (float) newFood;
        float saturation = newSaturation;

        if (food >= 18.0f && saturation > 0.0f) {
            float foodBuffer = Math.min(2.0f, food - 18.0f);

            float baseRate = 0.30f;
            float multiplier = 1.0f + (foodBuffer * 0.45f);
            float healthFromSaturation = saturation * baseRate * multiplier;
            float foodCost = saturation * 0.15f;

            totalHealthRestore += healthFromSaturation;
            food = Math.max(0, food - foodCost);
            saturation = 0.0f;
        }
        if (food >= 18.0f && saturation <= 0.0f) {
            float foodBuffer = food - 18.0f;
            float healthFromSlowRegen = foodBuffer / 1.5f;
            totalHealthRestore += healthFromSlowRegen;
        }
        return totalHealthRestore - 1.0f;
    }

    /**
     * Calculates pulsing alpha value for animation effect.
     */
    private static float getPulsingAlpha(float tickDelta) {
        long time = System.currentTimeMillis();
        double phase = (time % 1000) / 1000.0; // 1 second cycle
        return (float) (0.4 + 0.3 * Math.sin(phase * Math.PI * 2)); // Pulse between 0.4 and 0.7
    }

    /**
     * Renders health restore icons with pulsing animation and jitter matching vanilla.
     * Uses the same random instance as vanilla for synchronized animations which for some reason doesn't wanna work.
     */
    private static void renderHealthRestoreIcons(GuiGraphics guiGraphics, Player player, float currentHealth,
                                                 float healthRestore, int leftX, int baseY, int tickCount, RandomSource random,
                                                 float alpha, boolean hardcore) {
        float modifiedHealth = Math.min(currentHealth + healthRestore, player.getMaxHealth());

        if (modifiedHealth <= currentHealth) return;

        int currentHealthCeil = (int) Math.ceil(currentHealth);
        int modifiedHealthCeil = (int) Math.ceil(modifiedHealth);

        // Calculate overflow levels
        int currentOverflowLevel = currentHealthCeil / MAX_BASE_HEALTH;
        int currentEffectiveHealth = currentHealthCeil % MAX_BASE_HEALTH;
        if (currentEffectiveHealth == 0 && currentHealthCeil > 0) {
            currentOverflowLevel--;
            currentEffectiveHealth = MAX_BASE_HEALTH;
        }

        int startHealthBar = (int) Math.max(0, Math.ceil(currentHealth) / 2.0F);
        int endHealthBar = (int) Math.max(0, Math.ceil(modifiedHealth / 2.0F));

        // Match vanilla jitter animation exactly
        // Vanilla jitters when health <= 4 (2 hearts)
        boolean shouldAnimate = currentHealthCeil <= 4;

        // Create alpha color for rendering
        int alphaColor = ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);

        // Determine color index for overflow
        int colorIndex = currentOverflowLevel > 0 ? (currentOverflowLevel - 1) % 4 : -1;

        for (int i = startHealthBar; i < endHealthBar && i < MAX_ICONS; i++) {
            int x = leftX + (i % 10) * 8;
            int y = baseY;

            // Apply jitter animation matching vanilla exactly
            // Use the same random that vanilla uses (already seeded by Gui.renderPlayerHealth)
            if (shouldAnimate) {
                y += random.nextInt(2); // Vanilla uses nextInt(2) for vertical jitter
            }

            // Determine if half or full heart
            boolean isHalf = (i * 2 + 1) == modifiedHealthCeil;

            Identifier iconSprite;
            if (colorIndex >= 0) {
                iconSprite = isHalf ? getHalfHeartTexture(colorIndex, hardcore) : getFullHeartTexture(colorIndex, hardcore);
            } else {
                iconSprite = isHalf ? getVanillaHalfHeartTexture(hardcore) : getVanillaFullHeartTexture(hardcore);
            }

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, ICON_SIZE, ICON_SIZE, alphaColor);
        }
    }

    /**
     * Renders food restore icons with pulsing animation using vanilla food textures.
     */
    private static void renderFoodRestoreIcons(GuiGraphics guiGraphics, Player player, int currentFood,
                                               int foodRestore, int rightX, int baseY, int tickCount, RandomSource random,
                                               float alpha, boolean hasHunger) {
        if (foodRestore <= 0) return;

        FoodData foodData = player.getFoodData();
        int modifiedFood = Math.min(20, currentFood + foodRestore);

        int startFoodBar = Math.max(0, currentFood / 2);
        int endFoodBar = (int) Math.ceil(modifiedFood / 2.0F);

        // Determine which textures to use based on hunger effect
        Identifier fullTexture = hasHunger ? FOOD_FULL_HUNGER : FOOD_FULL;
        Identifier halfTexture = hasHunger ? FOOD_HALF_HUNGER : FOOD_HALF;

        // Determine if we should animate (jitter when saturation is low)
        boolean shouldAnimate = foodData.getSaturationLevel() <= 0.0F && tickCount % (currentFood * 3 + 1) == 0;

        // Create alpha color for rendering
        int alphaColor = ARGB.colorFromFloat(alpha, 1.0F, 1.0F, 1.0F);

        for (int i = startFoodBar; i < endFoodBar && i < MAX_ICONS; i++) {
            int x = rightX - i * 8 - 9;
            int y = baseY;

            // Apply jitter animation matching vanilla
            if (shouldAnimate) {
                y += random.nextInt(3) - 1;
            }

            // Determine if half or full icon
            boolean isHalf = (i * 2 + 1) == modifiedFood;
            Identifier iconSprite = isHalf ? halfTexture : fullTexture;

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, ICON_SIZE, ICON_SIZE, alphaColor);
        }
    }

    /**
     * Gets the appropriate full heart texture based on color and hardcore.
     * Uses the same logic as ClientHealthHudHelper.
     */
    private static Identifier getFullHeartTexture(int colorIndex, boolean hardcore) {
        String color = getColorName(colorIndex);
        String prefix = hardcore ? "hardcore_" : "";

        return Identifier.fromNamespaceAndPath(
                PackTools.MOD_ID,
                "textures/gui/hud/health/" + prefix + "full_" + color + ".png"
        );
    }

    /**
     * Gets the appropriate half heart texture based on color and hardcore.
     * Uses the same logic as ClientHealthHudHelper.
     */
    private static Identifier getHalfHeartTexture(int colorIndex, boolean hardcore) {
        String color = getColorName(colorIndex);
        String prefix = hardcore ? "hardcore_" : "";

        return Identifier.fromNamespaceAndPath(
                PackTools.MOD_ID,
                "textures/gui/hud/health/" + prefix + "half_" + color + ".png"
        );
    }

    /**
     * Gets vanilla red full heart texture.
     */
    private static Identifier getVanillaFullHeartTexture(boolean hardcore) {
        return hardcore ? Identifier.withDefaultNamespace("hud/heart/hardcore_full") : Identifier.withDefaultNamespace("hud/heart/full");
    }

    /**
     * Gets vanilla red half heart texture.
     */
    private static Identifier getVanillaHalfHeartTexture(boolean hardcore) {
        return hardcore ? Identifier.withDefaultNamespace("hud/heart/hardcore_half") : Identifier.withDefaultNamespace("hud/heart/half");
    }

    /**
     * Gets the color name for the given overflow index.
     * Uses the same color cycling as ClientHealthHudHelper.
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