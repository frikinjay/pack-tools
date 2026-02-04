package com.frikinjay.packtools.features.modnametooltips;

import com.frikinjay.packtools.platform.CommonPlatformHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public class ModNameTooltipHelper {

    public static Component createModNameTooltip(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        String modId = BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
        String modName = CommonPlatformHelper.getModName(modId);

        if (Objects.equals(modId, "minecraft")) {
            modName = "Minecraft";
        }

        return Component.literal(modName).withStyle(ChatFormatting.BLUE);
    }
}