package com.frikinjay.packtools.fabric;

import com.frikinjay.packtools.fabric.network.PTServerNetworkingFabric;
import net.fabricmc.api.DedicatedServerModInitializer;

public class PackToolsServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        PTServerNetworkingFabric.init();
    }
}
