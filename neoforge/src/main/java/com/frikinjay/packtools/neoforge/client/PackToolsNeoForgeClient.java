package com.frikinjay.packtools.neoforge.client;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.features.effectstooltips.ClientEffectsTooltipHelper;
import com.frikinjay.packtools.client.features.foodtooltips.ClientFoodTooltipHelper;
import com.frikinjay.packtools.util.tooltip.EffectsTooltip;
import com.frikinjay.packtools.util.tooltip.FoodTooltip;
import com.frikinjay.packtools.util.tooltip.MultiTooltipData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

@Mod(value = PackTools.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = PackTools.MOD_ID, value = Dist.CLIENT)
public class PackToolsNeoForgeClient {

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        /*event.enqueueWork(() ->

        );*/
    }

    @SubscribeEvent
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(FoodTooltip.class, ClientFoodTooltipHelper::createClientComponent);
        event.register(EffectsTooltip.class, ClientEffectsTooltipHelper::createClientComponent);
        event.register(MultiTooltipData.class, ClientFoodTooltipHelper::createClientComponent);
    }

}
