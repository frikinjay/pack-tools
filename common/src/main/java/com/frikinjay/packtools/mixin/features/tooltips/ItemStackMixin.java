package com.frikinjay.packtools.mixin.features.tooltips;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.durabilitytooltips.DurabilityTooltipHelper;
import com.frikinjay.packtools.features.modnametooltips.ModNameTooltipHelper;
import com.frikinjay.packtools.features.tagtooltips.TagTooltipsHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Shadow
    public abstract boolean isDamageableItem();

    @Inject(
            method = "getTooltipLines",
            at = @At("RETURN")
    )
    private void packtools$addCustomTooltips(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        List<Component> tooltips = cir.getReturnValue();

        // Ordered tooltips

        // 1. Durability Tooltip
        if (ConfigRegistry.DURABILITY_TOOLTIP.getValue() && !flag.isAdvanced() && this.isDamageableItem()) {
            Component durabilityTooltip = DurabilityTooltipHelper.createDurabilityTooltip(stack);
            if (durabilityTooltip != null) {
                tooltips.add(durabilityTooltip);
            }
        }

        // 2. Mod Name Tooltip
        if (ConfigRegistry.MOD_NAME_DISPLAY.getValue()) {
            Component modNameTooltip = ModNameTooltipHelper.createModNameTooltip(stack);
            if (modNameTooltip != null) {
                // Only add if not already present (case-sensitive)
                String modNameText = modNameTooltip.getString();
                boolean alreadyPresent = tooltips.stream()
                        .anyMatch(tooltip -> tooltip.getString().equals(modNameText));

                if (!alreadyPresent) {
                    tooltips.add(modNameTooltip);
                }
            }
        }

        // 3. Tag Tooltips
        if (ConfigRegistry.TAG_TOOLTIPS.getValue() && flag.isAdvanced()) {
            List<Component> tagComponents = TagTooltipsHelper.getTagComponents(stack, tooltips, context);
            if (tagComponents != null && !tagComponents.isEmpty()) {
                tooltips.addAll(tagComponents);
            }
        }
    }
}