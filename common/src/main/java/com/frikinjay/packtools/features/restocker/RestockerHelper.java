package com.frikinjay.packtools.features.restocker;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.network.RestockerItemPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.frikinjay.packtools.PackTools.LOGGER;

/**
 * Common server-side networking logic for both Fabric and NeoForge.
 * Handles restock packets with proper validation and security.
 */
public class RestockerHelper {

    private static final Map<UUID, Long> lastRestockTime = new HashMap<>();
    private static final long REFILL_COOLDOWN_MS = 50; // Prevent spam

    /**
     * Call this on server tick to clean up old entries.
     * Should be called every ~60 seconds.
     */
    private static long lastCleanupTime = 0;

    // Add this check at the start of processRestock()
    public static void processRestock(RestockerItemPacket packet, ServerPlayer player) {
        if (!ConfigRegistry.RESTOCKER.getValue()) {
            return;
        }

        // Periodic cleanup every 60 seconds
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > 60000) {
            cleanupRateLimits();
            lastCleanupTime = currentTime;
        }

        // Rate limiting check
        if (!checkRateLimit(player)) {
            LOGGER.warn("Player {} is sending restock packets too quickly!", player.getName().getString());
            return;
        }

        // Basic packet validation
        if (!validatePacketStructure(packet, player)) {
            LOGGER.warn("Player {} sent invalid restock packet structure!", player.getName().getString());
            return;
        }

        Inventory inventory = player.getInventory();

        // Capture current state
        ItemStack sourceStack = inventory.getItem(packet.sourceSlot());

        // Validate current inventory state
        if (!validateInventoryState(packet, sourceStack, player)) {
            return;
        }

        // CRITICAL: Validate return item is legitimate
        if (packet.returnItem() && !validateReturnItem(packet, sourceStack, player)) {
            LOGGER.warn("Player {} sent invalid return item in restock packet!", player.getName().getString());
            return;
        }

        // Execute the restock
        executeRestock(packet, inventory, sourceStack, player);
    }

    /**
     * Rate limiting to prevent packet spam exploits.
     */
    private static boolean checkRateLimit(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastRestockTime.get(playerId);

        if (lastTime != null && (currentTime - lastTime) < REFILL_COOLDOWN_MS) {
            return false;
        }

        lastRestockTime.put(playerId, currentTime);
        return true;
    }

    /**
     * Validate packet structure and slot bounds.
     */
    private static boolean validatePacketStructure(RestockerItemPacket packet, ServerPlayer player) {
        Inventory inventory = player.getInventory();

        // Validate hand slot is actually a hand slot
        if (packet.handSlot() != inventory.getSelectedSlot() &&
                packet.handSlot() != Inventory.SLOT_OFFHAND) {
            LOGGER.warn("Invalid hand slot: {}", packet.handSlot());
            return false;
        }

        // Validate source slot bounds
        if (packet.sourceSlot() < 0 || packet.sourceSlot() >= 36) {
            LOGGER.warn("Invalid source slot: {}", packet.sourceSlot());
            return false;
        }

        // Source and hand slot must be different
        if (packet.sourceSlot() == packet.handSlot()) {
            LOGGER.warn("Source and hand slot are the same: {}", packet.sourceSlot());
            return false;
        }

        // Validate return slot if applicable
        if (packet.returnItem()) {
            if (packet.returnSlot() < 0 || packet.returnSlot() >= 36) {
                LOGGER.warn("Invalid return slot: {}", packet.returnSlot());
                return false;
            }
            // Validate return item isn't empty
            if (packet.returnStack().isEmpty()) {
                LOGGER.warn("Return item is empty but returnItem is true");
                return false;
            }
        }

        return true;
    }

    /**
     * CRITICAL SECURITY: Validate that the return item is actually in the hand slot.
     * This prevents item creation exploits where clients lie about what's in their hand.
     */
    private static boolean validateReturnItem(RestockerItemPacket packet, ItemStack sourceStack, ServerPlayer player) {
        ItemStack returnStack = packet.returnStack();
        Inventory inventory = player.getInventory();

        // CRITICAL: Verify the return item is actually in the hand slot right now
        ItemStack currentHandStack = inventory.getItem(packet.handSlot());

        if (!ItemStack.isSameItemSameComponents(currentHandStack, returnStack)) {
            LOGGER.warn("Player {} sent return item {} but hand slot contains {}",
                    player.getName().getString(),
                    returnStack.getItem(),
                    currentHandStack.getItem());
            return false;
        }

        // Verify counts match (prevent duplication by claiming more than exists)
        if (currentHandStack.getCount() != returnStack.getCount()) {
            LOGGER.warn("Player {} sent return item count {} but hand slot has {}",
                    player.getName().getString(),
                    returnStack.getCount(),
                    currentHandStack.getCount());
            return false;
        }

        // Now validate the transformation is legitimate
        // Check for use remainder component (e.g., honey bottle -> empty bottle)
        if (sourceStack.has(DataComponents.USE_REMAINDER)) {
            var useRemainder = sourceStack.get(DataComponents.USE_REMAINDER);

            if (useRemainder != null) {
                ItemStack expectedRemainder = useRemainder.convertInto();

                if (!expectedRemainder.isEmpty()) {
                    // Validate the return item matches expected remainder
                    if (!ItemStack.isSameItemSameComponents(returnStack, expectedRemainder)) {
                        LOGGER.warn("Player {} has {} in hand but expected use remainder is {}",
                                player.getName().getString(),
                                returnStack.getItem(),
                                expectedRemainder.getItem());
                        return false;
                    }
                    return true;
                }
            }
        }

        // Check crafting remainder for items like buckets
        ItemStack craftingRemainder = sourceStack.getItem().getCraftingRemainder();
        if (!craftingRemainder.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(returnStack, craftingRemainder)) {
                LOGGER.warn("Player {} has {} in hand but expected crafting remainder is {}",
                        player.getName().getString(),
                        returnStack.getItem(),
                        craftingRemainder.getItem());
                return false;
            }
            return true;
        }

        // No remainder component - allow ANY transformation since:
        // 1. We verified the item is actually in their hand (not fabricated)
        // 2. We're just moving existing items, not creating new ones
        // 3. The worst case is they swap items inefficiently (no duplication possible)
        LOGGER.debug("Accepting verified transformation for player {}: {} -> {}",
                player.getName().getString(),
                sourceStack.getItem(),
                returnStack.getItem());
        return true;
    }

    /**
     * Validate that the inventory state is reasonable for a restock.
     * This prevents exploits while allowing legitimate timing differences.
     */
    private static boolean validateInventoryState(RestockerItemPacket packet,
                                                  ItemStack sourceStack,
                                                  ServerPlayer player) {
        // Source slot must not be empty
        if (sourceStack.isEmpty()) {
            LOGGER.debug("Source slot {} is empty for player {}",
                    packet.sourceSlot(), player.getName().getString());
            return false;
        }

        return true;
    }

    /**
     * Execute the restock operation after all validation passes.
     * For transformations: entire source stack goes to hand, item in hand goes to source slot
     */
    private static void executeRestock(RestockerItemPacket packet,
                                       Inventory inventory,
                                       ItemStack sourceStack,
                                       ServerPlayer player) {

        // For item transformations (e.g., honey bottle -> empty bottle, empty bucket -> water bucket):
        if (packet.returnItem() && !packet.returnStack().isEmpty()) {
            // Get current hand contents for final validation
            ItemStack currentHandStack = inventory.getItem(packet.handSlot());

            // SECURITY: Double-check hand slot still contains the claimed return item
            if (!ItemStack.isSameItemSameComponents(currentHandStack, packet.returnStack()) ||
                    currentHandStack.getCount() != packet.returnStack().getCount()) {
                LOGGER.warn("Hand slot changed during processing for player {} - possible race condition or exploit attempt",
                        player.getName().getString());
                return;
            }

            // Take entire stack from source slot for the hand
            ItemStack replacement = sourceStack.copy();
            ItemStack toReturn = currentHandStack.copy(); // Use actual hand contents, not packet data

            // Clear source slot (entire stack is being moved)
            inventory.setItem(packet.sourceSlot(), ItemStack.EMPTY);

            // Place replacement (entire stack) in hand slot
            inventory.setItem(packet.handSlot(), replacement);

            // Place return item (e.g., empty bottle or water bucket) in the now-empty source slot
            inventory.setItem(packet.returnSlot(), toReturn);
        } else {
            // Simple restock: just move entire stack from source to hand
            ItemStack replacement = sourceStack.copy();
            inventory.setItem(packet.sourceSlot(), ItemStack.EMPTY);
            inventory.setItem(packet.handSlot(), replacement);
        }

        // Sync inventory to client
        player.inventoryMenu.broadcastFullState();
    }

    /**
     * Cleanup old rate limit entries to prevent memory leak.
     * Call this periodically (e.g., on player disconnect or server tick).
     */
    public static void cleanupRateLimits() {
        long currentTime = System.currentTimeMillis();
        lastRestockTime.entrySet().removeIf(entry ->
                currentTime - entry.getValue() > 60000); // Remove entries older than 1 minute
    }

    /**
     * Remove rate limit entry for a specific player (call on disconnect).
     */
    public static void removePlayerRateLimit(UUID playerId) {
        lastRestockTime.remove(playerId);
    }
}