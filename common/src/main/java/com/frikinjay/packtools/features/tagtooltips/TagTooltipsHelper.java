package com.frikinjay.packtools.features.tagtooltips;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class TagTooltipsHelper {

    public static List<Component> getTagComponents(ItemStack stack, List<Component> tooltips, Item.TooltipContext context) {
        if (stack.isEmpty()) {
            return null;
        }

        List<Component> tagTooltips = new ArrayList<>(List.of());

        // Get item tags
        List<Component> itemTagComponents = getItemTags(stack, context);

        // Get block tags if applicable
        List<Component> blockTagComponents = getBlockTags(stack, context);

        // Add item tags section
        if (!itemTagComponents.isEmpty()) {
            //tagTooltips.add(Component.empty());
            tagTooltips.add(Component.literal("Item Tags:").withStyle(ChatFormatting.GRAY));
            tagTooltips.addAll(itemTagComponents);
        }

        // Add block tags section
        if (!blockTagComponents.isEmpty()) {
            if (itemTagComponents.isEmpty()) {
                //tagTooltips.add(Component.empty());
            }
            tagTooltips.add(Component.literal("Block Tags:").withStyle(ChatFormatting.GRAY));
            tagTooltips.addAll(blockTagComponents);
        }
        return tagTooltips;
    }

    private static List<Component> getItemTags(ItemStack stack, Item.TooltipContext context) {
        return stack.getItem()
                .builtInRegistryHolder()
                .tags()
                .map(TagTooltipsHelper::formatTag)
                .toList();
    }

    private static List<Component> getBlockTags(ItemStack stack, Item.TooltipContext context) {
        Block block = Block.byItem(stack.getItem());

        if (block == null || block.defaultBlockState().isAir()) {
            return List.of();
        }

        return block.builtInRegistryHolder()
                .tags()
                .map(TagTooltipsHelper::formatTag)
                .toList();
    }

    private static Component formatTag(TagKey<?> tagKey) {
        Identifier location = tagKey.location();
        String namespace = location.getNamespace();
        String path = location.getPath();

        // Format: namespace:path with color coding
        Component namespaceComponent = Component.literal(namespace + ":")
                .withStyle(ChatFormatting.DARK_GRAY);
        Component pathComponent = Component.literal(path)
                .withStyle(ChatFormatting.GRAY);

        return Component.literal("  • ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(namespaceComponent)
                .append(pathComponent);
    }
}