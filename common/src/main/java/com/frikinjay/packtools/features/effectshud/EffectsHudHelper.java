package com.frikinjay.packtools.features.effectshud;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Shared logic for effects HUD - safe for both client and server.
 */
public class EffectsHudHelper {

    private static class PlayerEffectData {
        final Map<Holder<MobEffect>, Long> applicationTimes = new HashMap<>();
        List<MobEffectInstance> cachedSortedEffects = new ArrayList<>();
        Set<Holder<MobEffect>> lastEffectSet = new HashSet<>();
        long lastCleanupCheck = 0;
    }

    private static final Map<String, PlayerEffectData> playerData = new HashMap<>();
    private static long applicationCounter = 0;
    private static final long CLEANUP_INTERVAL = 6000; // Clean up every ~5 minutes (ticks)

    /**
     * Gets active effects from the player, filtered and sorted by application time.
     * Newer effects appear first (leftmost), older effects appear last (rightmost).
     */
    public static List<MobEffectInstance> getActiveEffects(Player player) {
        if (!ConfigRegistry.EFFECTS_HUD.getValue()) {
            return List.of();
        }

        Collection<MobEffectInstance> effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            playerData.remove(player.getStringUUID());
            return List.of();
        }

        String playerKey = player.getStringUUID();
        PlayerEffectData data = playerData.computeIfAbsent(playerKey, k -> new PlayerEffectData());

        // Periodic cleanup of disconnected players (client-side only needs current player)
        long currentTime = System.currentTimeMillis();
        if (currentTime - data.lastCleanupCheck > CLEANUP_INTERVAL) {
            data.lastCleanupCheck = currentTime;
            periodicCleanup(player);
        }

        // Build current effect set
        Set<Holder<MobEffect>> currentEffectSet = new HashSet<>();
        for (MobEffectInstance effect : effects) {
            currentEffectSet.add(effect.getEffect());
        }

        // Check if effects changed
        boolean effectsChanged = !currentEffectSet.equals(data.lastEffectSet);

        if (effectsChanged) {
            // Track new effects
            for (MobEffectInstance effect : effects) {
                Holder<MobEffect> effectHolder = effect.getEffect();
                if (!data.applicationTimes.containsKey(effectHolder)) {
                    data.applicationTimes.put(effectHolder, applicationCounter++);
                }
            }

            // Remove expired effects
            data.applicationTimes.keySet().retainAll(currentEffectSet);

            // Re-sort and cache (oldest first = leftmost, newest last = rightmost)
            data.cachedSortedEffects = new ArrayList<>(effects);
            data.cachedSortedEffects.sort((e1, e2) -> {
                Long time1 = data.applicationTimes.get(e1.getEffect());
                Long time2 = data.applicationTimes.get(e2.getEffect());
                return Long.compare(time1, time2);
            });

            data.lastEffectSet = currentEffectSet;
        }

        return data.cachedSortedEffects;
    }

    /**
     * Calculates how many rows are needed for the given number of effects.
     */
    public static int calculateRowCount(int effectCount, int maxPerRow) {
        if (effectCount == 0) return 0;
        return (int) Math.ceil((double) effectCount / maxPerRow);
    }

    /**
     * Periodic cleanup to remove stale player data (for multiplayer).
     * On client-side, keeps only the current player.
     */
    private static void periodicCleanup(Player currentPlayer) {
        if (playerData.size() <= 1) return; // No cleanup needed

        String currentPlayerKey = currentPlayer.getStringUUID();
        playerData.keySet().removeIf(key -> !key.equals(currentPlayerKey));
    }

    /**
     * Cleans up tracking data for a specific player.
     * Call this when a player disconnects (server-side).
     */
    public static void cleanupPlayer(Player player) {
        playerData.remove(player.getStringUUID());
    }

    /**
     * Clears all tracking data.
     */
    public static void clearAllTracking() {
        playerData.clear();
        applicationCounter = 0;
    }
}