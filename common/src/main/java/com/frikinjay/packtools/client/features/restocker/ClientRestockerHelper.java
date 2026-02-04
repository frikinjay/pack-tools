package com.frikinjay.packtools.client.features.restocker;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.network.RestockerItemPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Optimized client-side slot restocker.
 * Handles automatic inventory restocking with improved performance.
 */
public class ClientRestockerHelper {
    private static final Deque<RestockAction> scheduledRestocks = new ArrayDeque<>();
    private static boolean processingServerUpdate = false;

    private ClientRestockerHelper() {}

    public static void setProcessingServerUpdate(boolean processing) {
        processingServerUpdate = processing;
    }

    public static boolean isProcessingServerUpdate() {
        return processingServerUpdate;
    }

    public static boolean scheduleRestockChecked(InteractionHand hand, Inventory inventory,
                                                 ItemStack before, ItemStack after) {
        if (!ConfigRegistry.RESTOCKER.getValue() || processingServerUpdate) {
            return false;
        }

        // Check if items are the same type
        if (ItemStack.isSameItem(before, after)) {
            // Only restock if count decreased or slot became empty
            if (!after.isEmpty() && after.getCount() < before.getCount()) {
                return false; // Just count decreased, not empty yet
            }
            if (after.isEmpty()) {
                return scheduleRestockUnchecked(hand, inventory, before, after);
            }
        }

        // Different items - likely a transformation (e.g., bowl after soup, bottle after potion)
        if (!ItemStack.isSameItem(before, after) && !before.isEmpty()) {
            return scheduleRestockUnchecked(hand, inventory, before, after);
        }

        return false;
    }

    public static boolean scheduleRestockUnchecked(InteractionHand hand, Inventory inventory,
                                                   ItemStack reference, ItemStack afterItem) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }

        // Prevent duplicate actions in queue
        RestockAction newAction = new RestockAction(hand, inventory, reference.copy(), afterItem.copy());

        // Don't add if identical action already queued
        for (RestockAction action : scheduledRestocks) {
            if (action.isDuplicate(newAction)) {
                return false;
            }
        }

        scheduledRestocks.offer(newAction);
        return true;
    }

    public static void performRestock() {
        if (scheduledRestocks.isEmpty()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            scheduledRestocks.clear();
            return;
        }

        // Process all queued actions (server-side rate limiting will handle spam)
        while (!scheduledRestocks.isEmpty()) {
            RestockAction action = scheduledRestocks.poll();
            if (action != null) {
                action.execute();
            }
        }
    }

    public static void clear() {
        scheduledRestocks.clear();
    }

    private record RestockAction(InteractionHand hand, Inventory inventory, ItemStack reference, ItemStack afterItem) {

        /**
         * Check if this action is a duplicate of another.
         */
        boolean isDuplicate(RestockAction other) {
            return this.hand == other.hand &&
                    ItemStack.isSameItem(this.reference, other.reference);
        }

        void execute() {
            int handSlot = hand == InteractionHand.MAIN_HAND
                    ? inventory.getSelectedSlot()
                    : Inventory.SLOT_OFFHAND;

            ReplacementResult result = findReplacementWithSlot();

            if (result != null) {
                boolean hasReturnItem = reference.getCount() == 1 &&
                        !afterItem.isEmpty() &&
                        !ItemStack.isSameItem(afterItem, reference);

                //ItemStack sourceItem = inventory.getItem(result.sourceSlot());
                //PackTools.LOGGER.debug("Client scheduling restock:");
                //PackTools.LOGGER.debug("  Reference: {} x{}", reference.getItem(), reference.getCount());
                //PackTools.LOGGER.debug("  After: {} x{}", afterItem.getItem(), afterItem.getCount());
                //PackTools.LOGGER.debug("  Source found: {} x{}", sourceItem.getItem(), sourceItem.getCount());
                //PackTools.LOGGER.debug("  hasReturnItem: {}", hasReturnItem);
                //PackTools.LOGGER.debug("  Source slot: {}", result.sourceSlot());

                RestockerItemPacket packet = new RestockerItemPacket(
                        handSlot,
                        result.sourceSlot(),
                        hasReturnItem,
                        hasReturnItem ? result.sourceSlot() : -1,
                        hasReturnItem ? afterItem.copy() : ItemStack.EMPTY
                );

                sendPacketToServer(packet);
            }
        }

        private void sendPacketToServer(RestockerItemPacket packet) {
            Minecraft client = Minecraft.getInstance();
            ClientPacketListener connection = client.getConnection();

            if (connection != null) {
                connection.send(new ServerboundCustomPayloadPacket(packet));
            }
        }

        @Nullable
        private ReplacementResult findReplacementWithSlot() {
            int handSlot = hand == InteractionHand.MAIN_HAND
                    ? inventory.getSelectedSlot()
                    : Inventory.SLOT_OFFHAND;

            // Search hotbar first (more likely to have duplicates)
            for (int i = 0; i < 9; i++) {
                if (i == handSlot) continue;
                ItemStack stack = inventory.getItem(i);
                if (areStacksCompatible(reference, stack)) {
                    return new ReplacementResult(stack.copy(), i);
                }
            }

            // Then search rest of inventory
            for (int i = 9; i < 36; i++) {
                if (i == handSlot) continue;
                ItemStack stack = inventory.getItem(i);
                if (areStacksCompatible(reference, stack)) {
                    return new ReplacementResult(stack.copy(), i);
                }
            }

            return null;
        }

        private boolean areStacksCompatible(ItemStack reference, ItemStack candidate) {
            if (reference.isEmpty() || candidate.isEmpty()) {
                return false;
            }

            if (reference.getItem() != candidate.getItem()) {
                return false;
            }

            if (reference.has(DataComponents.POTION_CONTENTS)) {
                return areComponentsEqual(reference, candidate, DataComponents.POTION_CONTENTS);
            }

            if (reference.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)) {
                return areComponentsEqual(reference, candidate, DataComponents.SUSPICIOUS_STEW_EFFECTS);
            }

            if (reference.has(DataComponents.CUSTOM_MODEL_DATA)) {
                return areComponentsEqual(reference, candidate, DataComponents.CUSTOM_MODEL_DATA);
            }

            return true;
        }

        private <T> boolean areComponentsEqual(ItemStack stack1, ItemStack stack2,
                                               DataComponentType<@NotNull T> componentType) {
            T component1 = stack1.get(componentType);
            T component2 = stack2.get(componentType);

            // Both null = equal
            if (component1 == null && component2 == null) {
                return true;
            }

            // One null = not equal
            if (component1 == null || component2 == null) {
                return false;
            }

            return component1.equals(component2);
        }
    }

    private record ReplacementResult(ItemStack replacement, int sourceSlot) {}
}