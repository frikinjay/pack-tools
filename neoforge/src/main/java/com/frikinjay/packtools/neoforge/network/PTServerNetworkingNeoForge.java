package com.frikinjay.packtools.neoforge.network;

import com.frikinjay.packtools.features.restocker.RestockerHelper;
import com.frikinjay.packtools.network.RestockerItemPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * NeoForge-specific networking handler.
 * Delegates to common PTServerNetworking for actual logic.
 */
public class PTServerNetworkingNeoForge {

    /**
     * Handle restock packet - called from PTNetworkingNeoForge.
     * Queues work on the server thread via the context.
     */
    public static void handleRestockPacket(RestockerItemPacket packet,
                                           net.neoforged.neoforge.network.handling.IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        // Execute on server thread
        context.enqueueWork(() -> RestockerHelper.processRestock(packet, player));
    }
}