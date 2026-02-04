package com.frikinjay.packtools.neoforge.network;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.network.RestockerItemPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class PTNetworkingNeoForge {

    public static void init(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PackTools.MOD_ID);

        registrar.playToServer(
                RestockerItemPacket.TYPE,
                RestockerItemPacket.STREAM_CODEC,
                PTServerNetworkingNeoForge::handleRestockPacket
        );
    }
}
