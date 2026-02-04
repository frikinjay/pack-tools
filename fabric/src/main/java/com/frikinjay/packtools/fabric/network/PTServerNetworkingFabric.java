package com.frikinjay.packtools.fabric.network;

import com.frikinjay.packtools.features.restocker.RestockerHelper;
import com.frikinjay.packtools.network.RestockerItemPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric-specific networking handler.
 * Delegates to common PTServerNetworking for actual logic.
 */
public class PTServerNetworkingFabric {

    /**
     * Initialize server-side packet handlers.
     */
    public static void init() {
        // Register packet handler
        ServerPlayNetworking.registerGlobalReceiver(
                RestockerItemPacket.TYPE,
                (payload, context) -> {
                    ServerPlayer player = context.player();
                    // Execute on server thread
                    context.server().execute(() ->
                            RestockerHelper.processRestock(payload, player));
                }
        );

        // Cleanup rate limits on player disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            RestockerHelper.removePlayerRateLimit(handler.getPlayer().getUUID());
        });
    }
}