package com.frikinjay.packtools.config;

import java.util.*;

public class ConfigRegistry {

    private static final Map<String, ConfigEntry<?>> ENTRIES = new LinkedHashMap<>();

    // Category names
    public static final String CATEGORY_VALUES = "Values";
    public static final String CATEGORY_FEATURES = "Features";
    public static final String CATEGORY_TOOLTIPS = "Tooltips";
    public static final String CATEGORY_HUD = "HUD";

    // ===== Config Values =====
    public static final ConfigEntry<Integer> MENDING_REPAIR_XP_RATIO = register(
            new ConfigEntry<>("mendingRepairXpRatio", "XP to durability ratio for mending (xp:durability)", CATEGORY_VALUES, 2, Integer.class, 1, 100)
    );

    public static final ConfigEntry<Integer> PIGLIN_TRADE_DURATION = register(
            new ConfigEntry<>("piglinTradeDuration", "Duration piglins take to throw bartered items to player in ticks", CATEGORY_VALUES, 34, Integer.class, 1, 200)
    );

    // ===== Feature Toggles =====
    public static final ConfigEntry<Boolean> PING = register(
            new ConfigEntry<>("pingEnabled", "Enable startup ping notification", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> GLOBAL_SOUNDS_SUPPRESSOR = register(
            new ConfigEntry<>("globalSoundsSuppressorEnabled", "Suppress global sounds", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> EXPERIMENTAL_SUPPRESSOR = register(
            new ConfigEntry<>("experimentalSuppressorEnabled", "Suppress experimental gameplay warnings", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> DRAGON_EGG = register(
            new ConfigEntry<>("dragonEggEnabled", "Enable dragon egg respawner", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> CLUMPED = register(
            new ConfigEntry<>("clumpedEnabled", "Enable xp clumping", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> EASY_HARVEST = register(
            new ConfigEntry<>("easyHarvestEnabled", "Enable easy harvesting", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> JUMP_OVER = register(
            new ConfigEntry<>("jumpOverEnabled", "Enable jumping over fences/walls", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> QUICK_ZOOM = register(
            new ConfigEntry<>("quickZoomEnabled", "Enable quick zoom feature", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> REACHER = register(
            new ConfigEntry<>("reacherEnabled", "Enable quark style reach around place feature", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> SWING_THROUGH = register(
            new ConfigEntry<>("swingThroughEnabled", "Enable swing through grass/foliage", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> DOUBLE_DOORS = register(
            new ConfigEntry<>("doubleDoorsEnabled", "Enable automatic double door opening", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> RESTOCKER = register(
            new ConfigEntry<>("restockerEnabled", "Enable automatic item restocking", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> TOAST_SUPPRESSOR = register(
            new ConfigEntry<>("toastSuppressorEnabled", "Suppress tutorial and recipe toast notifications", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> NO_TRAMPLE = register(
            new ConfigEntry<>("noTrampleEnabled", "Prevent farmland trampling", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> STACK_MAXXER = register(
            new ConfigEntry<>("stackMaxxerEnabled", "Enable stack size maximizer", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> RAPID_PIGLIN = register(
            new ConfigEntry<>("rapidPiglinEnabled", "Enable rapid piglin trading", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> MOUSE_AND_KEY_TWEAKS = register(
            new ConfigEntry<>("mouseAndKeyTweaksEnabled", "Enable mouse and key tweaks", CATEGORY_FEATURES, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> ELYTRA_REPLENISHER = register(
            new ConfigEntry<>("elytraReplenisherEnabled", "Enable elytra replenisher for end ships", CATEGORY_FEATURES, true, Boolean.class)
    );

    // ===== Tooltip Toggles =====
    public static final ConfigEntry<Boolean> MOD_NAME_DISPLAY = register(
            new ConfigEntry<>("modNameDisplayEnabled", "Show mod name in tooltips", CATEGORY_TOOLTIPS, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> DURABILITY_TOOLTIP = register(
            new ConfigEntry<>("durabilityTooltipEnabled", "Show durability in tooltips", CATEGORY_TOOLTIPS, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> TAG_TOOLTIPS = register(
            new ConfigEntry<>("tagTooltipsEnabled", "Show tags in tooltips (After F3+H)", CATEGORY_TOOLTIPS, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> FOOD_TOOLTIPS = register(
            new ConfigEntry<>("foodToolTipsEnabled", "Show food information in tooltips", CATEGORY_TOOLTIPS, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> EFFECTS_TOOLTIPS = register(
            new ConfigEntry<>("effectsTooltipsEnabled", "Show effect information in tooltips", CATEGORY_TOOLTIPS, true, Boolean.class)
    );

    // ===== HUD =====
    public static final ConfigEntry<Boolean> EFFECTS_HUD = register(
            new ConfigEntry<>("effectsHudEnabled", "Show status effects on HUD", CATEGORY_HUD, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> ARMOR_HUD = register(
            new ConfigEntry<>("armorHudEnabled", "Show armor value on HUD", CATEGORY_HUD, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> TOUGHNESS_HUD = register(
            new ConfigEntry<>("toughnessHudEnabled", "Show armor toughness value on HUD", CATEGORY_HUD, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> HEALTH_HUD = register(
            new ConfigEntry<>("healthHudEnabled", "Show overlayed health hearts on HUD", CATEGORY_HUD, true, Boolean.class)
    );

    public static final ConfigEntry<Boolean> RESTORE_HUD = register(
            new ConfigEntry<>("restoreHudEnabled", "Show overlayed pulsing health hearts and food sprites on HUD for potential health and food gain from holding consumable", CATEGORY_HUD, true, Boolean.class)
    );

    private static <T> ConfigEntry<T> register(ConfigEntry<T> entry) {
        ENTRIES.put(entry.getName(), entry);
        return entry;
    }

    public static Collection<ConfigEntry<?>> getAllEntries() {
        return ENTRIES.values();
    }

    public static ConfigEntry<?> getEntry(String name) {
        return ENTRIES.get(name);
    }

    public static List<ConfigEntry<Boolean>> getBooleanEntries() {
        List<ConfigEntry<Boolean>> list = new ArrayList<>();
        for (ConfigEntry<?> entry : ENTRIES.values()) {
            if (entry.getType() == Boolean.class) {
                list.add((ConfigEntry<Boolean>) entry);
            }
        }
        return list;
    }

    public static List<ConfigEntry<Integer>> getIntegerEntries() {
        List<ConfigEntry<Integer>> list = new ArrayList<>();
        for (ConfigEntry<?> entry : ENTRIES.values()) {
            if (entry.getType() == Integer.class) {
                list.add((ConfigEntry<Integer>) entry);
            }
        }
        return list;
    }
}