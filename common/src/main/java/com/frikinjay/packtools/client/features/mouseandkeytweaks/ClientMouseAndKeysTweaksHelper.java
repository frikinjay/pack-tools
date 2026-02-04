package com.frikinjay.packtools.client.features.mouseandkeytweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ClientMouseAndKeysTweaksHelper {

    public static final ClientMouseAndKeysTweaksHelper INSTANCE = new ClientMouseAndKeysTweaksHelper();

    private boolean leftMousePressed = false;
    private Slot lastProcessedSlot = null;

    public void onMousePressed(int button) {
        if (button == 0) {
            this.leftMousePressed = true;
            this.lastProcessedSlot = null;
        }
    }

    public void onMouseReleased(int button) {
        if (button == 0) {
            this.leftMousePressed = false;
            this.lastProcessedSlot = null;
        }
    }

    /**
     * Handles shift+drag hover behavior - emulates vanilla shift+click on hovered slots
     */
    public void onRender(AbstractContainerScreen<?> screen, Slot hoveredSlot) {
        if (!this.leftMousePressed || !Minecraft.getInstance().hasShiftDown()) {
            return;
        }

        if (screen.getMenu().getCarried().isEmpty() && hoveredSlot != null &&
                hoveredSlot.hasItem() && hoveredSlot != this.lastProcessedSlot) {

            var gameMode = Minecraft.getInstance().gameMode;
            var player = Minecraft.getInstance().player;

            if (gameMode != null && player != null) {
                gameMode.handleInventoryMouseClick(
                        screen.getMenu().containerId,
                        hoveredSlot.index,
                        0,
                        ClickType.QUICK_MOVE,
                        player
                );
                this.lastProcessedSlot = hoveredSlot;
            }
        }
    }

    /**
     * Handles mouse scroll to move single items between inventories
     */
    public boolean onMouseScrolled(AbstractContainerScreen<?> screen, Slot hoveredSlot, double scrollY) {
        if (hoveredSlot == null) return false;

        if (scrollY < 0 && hoveredSlot.hasItem()) {
            moveSingleItemOut(screen, hoveredSlot);
            return true;
        } else if (scrollY > 0) {
            moveSingleItemIn(screen, hoveredSlot);
            return true;
        }
        return false;
    }

    /**
     * Move 1 item from source slot to opposite inventory
     */
    private void moveSingleItemOut(AbstractContainerScreen<?> screen, Slot sourceSlot) {
        Slot targetSlot = findTargetSlot(screen, sourceSlot);
        if (targetSlot == null) return;

        var gameMode = Minecraft.getInstance().gameMode;
        var player = Minecraft.getInstance().player;
        if (gameMode == null || player == null) return;

        int containerId = screen.getMenu().containerId;

        // Pickup all -> Place one in target -> Return rest to source
        gameMode.handleInventoryMouseClick(containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
        gameMode.handleInventoryMouseClick(containerId, targetSlot.index, 1, ClickType.PICKUP, player);
        gameMode.handleInventoryMouseClick(containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
    }

    /**
     * Move 1 item from opposite inventory to target slot
     */
    private void moveSingleItemIn(AbstractContainerScreen<?> screen, Slot targetSlot) {
        var player = Minecraft.getInstance().player;
        var gameMode = Minecraft.getInstance().gameMode;
        if (player == null || gameMode == null) return;

        boolean targetIsPlayerInventory = targetSlot.container == player.getInventory();
        int containerId = screen.getMenu().containerId;

        for (Slot sourceSlot : screen.getMenu().slots) {
            boolean sourceIsPlayerInventory = sourceSlot.container == player.getInventory();

            if (sourceIsPlayerInventory == targetIsPlayerInventory || !sourceSlot.hasItem()) {
                continue;
            }

            boolean canStack = !targetSlot.hasItem() ||
                    (ItemStack.isSameItemSameComponents(sourceSlot.getItem(), targetSlot.getItem()) &&
                            targetSlot.getItem().getCount() < targetSlot.getItem().getMaxStackSize());

            if (canStack) {
                // Pickup all -> Place one in target -> Return rest to source
                gameMode.handleInventoryMouseClick(containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
                gameMode.handleInventoryMouseClick(containerId, targetSlot.index, 1, ClickType.PICKUP, player);
                gameMode.handleInventoryMouseClick(containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
                break;
            }
        }
    }

    /**
     * Find appropriate target slot in opposite inventory (prefers matching stacks, then empty slots)
     */
    private Slot findTargetSlot(AbstractContainerScreen<?> screen, Slot sourceSlot) {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;

        boolean sourceIsPlayerInventory = sourceSlot.container == player.getInventory();
        ItemStack sourceStack = sourceSlot.getItem();

        // First pass: find matching stacks
        for (Slot slot : screen.getMenu().slots) {
            boolean slotIsPlayerInventory = slot.container == player.getInventory();

            if (slotIsPlayerInventory != sourceIsPlayerInventory && slot.hasItem() &&
                    ItemStack.isSameItemSameComponents(slot.getItem(), sourceStack) &&
                    slot.getItem().getCount() < slot.getItem().getMaxStackSize()) {
                return slot;
            }
        }

        // Second pass: find empty slots
        for (Slot slot : screen.getMenu().slots) {
            boolean slotIsPlayerInventory = slot.container == player.getInventory();

            if (slotIsPlayerInventory != sourceIsPlayerInventory && !slot.hasItem() && slot.mayPlace(sourceStack)) {
                return slot;
            }
        }

        return null;
    }

    /**
     * Handles middle-click to sort container inventory
     */
    public void onMouseMiddleClicked(AbstractContainerScreen<?> screen, Slot hoveredSlot) {
        if (hoveredSlot != null && hoveredSlot.container != null) {
            sortContainer(screen, hoveredSlot.container);
        }
    }

    /**
     * Sort container by merging stacks and alphabetically ordering items
     */
    private void sortContainer(AbstractContainerScreen<?> screen, Container targetContainer) {
        var player = Minecraft.getInstance().player;
        var gameMode = Minecraft.getInstance().gameMode;
        if (player == null || gameMode == null) return;

        int containerId = screen.getMenu().containerId;
        boolean isPlayerInventory = targetContainer == player.getInventory();

        // Collect relevant slots (exclude hotbar and offhand for player inventory)
        List<Slot> relevantSlots = new ArrayList<>();
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == targetContainer) {
                int slotIndex = slot.getContainerSlot();

                if (isPlayerInventory && (slotIndex < 9 || slotIndex == 40)) {
                    continue; // Skip hotbar (0-8) and offhand (40)
                }
                relevantSlots.add(slot);
            }
        }

        // Phase 1: Merge partial stacks
        for (int i = 0; i < relevantSlots.size(); i++) {
            Slot target = relevantSlots.get(i);
            if (!target.hasItem() || target.getItem().getCount() >= target.getItem().getMaxStackSize()) {
                continue;
            }

            for (int j = i + 1; j < relevantSlots.size(); j++) {
                Slot source = relevantSlots.get(j);
                if (!source.hasItem()) continue;

                if (ItemStack.isSameItemSameComponents(target.getItem(), source.getItem())) {
                    gameMode.handleInventoryMouseClick(containerId, source.index, 0, ClickType.PICKUP, player);
                    gameMode.handleInventoryMouseClick(containerId, target.index, 0, ClickType.PICKUP, player);
                    gameMode.handleInventoryMouseClick(containerId, source.index, 0, ClickType.PICKUP, player);

                    if (target.getItem().getCount() >= target.getItem().getMaxStackSize()) break;
                }
            }
        }

        // Phase 2: Sort alphabetically (selection sort)
        for (int i = 0; i < relevantSlots.size(); i++) {
            Slot currentSlot = relevantSlots.get(i);
            int bestSlotIndex = i;

            for (int j = i + 1; j < relevantSlots.size(); j++) {
                Slot compareSlot = relevantSlots.get(j);
                if (!compareSlot.hasItem()) continue;

                if (!relevantSlots.get(bestSlotIndex).hasItem()) {
                    bestSlotIndex = j;
                    continue;
                }

                String nameA = relevantSlots.get(bestSlotIndex).getItem().getHoverName().getString().toLowerCase();
                String nameB = compareSlot.getItem().getHoverName().getString().toLowerCase();

                if (nameB.compareTo(nameA) < 0) {
                    bestSlotIndex = j;
                }
            }

            if (bestSlotIndex != i) {
                Slot sourceSlot = relevantSlots.get(bestSlotIndex);
                // Swap using cursor as buffer
                gameMode.handleInventoryMouseClick(containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
                gameMode.handleInventoryMouseClick(containerId, currentSlot.index, 0, ClickType.PICKUP, player);
                gameMode.handleInventoryMouseClick(containerId, sourceSlot.index, 0, ClickType.PICKUP, player);
            }
        }
    }
}