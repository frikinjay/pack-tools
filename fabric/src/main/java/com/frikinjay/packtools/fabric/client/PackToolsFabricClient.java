package com.frikinjay.packtools.fabric.client;

import com.frikinjay.packtools.client.features.effectstooltips.ClientEffectsTooltipHelper;
import com.frikinjay.packtools.client.features.foodtooltips.ClientFoodTooltipHelper;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

public final class PackToolsFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TooltipComponentCallback.EVENT.register(data -> {
            ClientTooltipComponent food = ClientFoodTooltipHelper.createClientComponent(data);
            if (food != null) return food;

            return ClientEffectsTooltipHelper.createClientComponent(data);
        });
    }
}
