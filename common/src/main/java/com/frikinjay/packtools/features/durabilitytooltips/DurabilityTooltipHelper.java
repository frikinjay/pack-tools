package com.frikinjay.packtools.features.durabilitytooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class DurabilityTooltipHelper {

    public static Component createDurabilityTooltip(ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return null;
        }
        int maxDamage = stack.getMaxDamage();
        int damage = stack.getDamageValue();
        int remaining = maxDamage - damage;
        double percentage = (double) remaining / maxDamage * 100.0;
        ChatFormatting color = getDurabilityColor(percentage);
        return Component.literal("Durability: ")
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(remaining))
                        .withStyle(color))
                .append(Component.literal(" / " + maxDamage)
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.format(" (%.1f%%)", percentage))
                        .withStyle(color));
    }

    private static ChatFormatting getDurabilityColor(double percentage) {
        if (percentage >= 75.0) {
            return ChatFormatting.GREEN;
        } else if (percentage >= 50.0) {
            return ChatFormatting.YELLOW;
        } else if (percentage >= 25.0) {
            return ChatFormatting.GOLD;
        } else {
            return ChatFormatting.RED;
        }
    }
}