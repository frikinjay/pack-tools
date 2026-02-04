package com.frikinjay.packtools.fabric;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.fabric.network.PTNetworkingFabric;
import com.frikinjay.packtools.fabric.network.PTServerNetworkingFabric;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class PackToolsFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        //AutoConfig.register(PTConfigFabric.class, GsonConfigSerializer::new);
        //ConfigHelperImpl.initPlatformConfig();
        PackTools.init();

        PTNetworkingFabric.init();

        PTServerNetworkingFabric.init();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
        });
    }
}
