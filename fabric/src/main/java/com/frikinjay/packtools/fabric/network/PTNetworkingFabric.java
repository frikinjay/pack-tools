package com.frikinjay.packtools.fabric.network;

import com.frikinjay.packtools.network.RestockerItemPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class PTNetworkingFabric {

    public static void init() {
        PayloadTypeRegistry.playC2S().register(RestockerItemPacket.TYPE, RestockerItemPacket.STREAM_CODEC);
    }
}
