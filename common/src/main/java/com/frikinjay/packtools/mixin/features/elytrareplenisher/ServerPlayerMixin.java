package com.frikinjay.packtools.mixin.features.elytrareplenisher;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.elytrareplenisher.ElytraReplenisherHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Unique
    private ChunkPos packtools$lastCheckedChunk = null;

    @Inject(method = "tick", at = @At("TAIL"))
    private void packtools$onTick(CallbackInfo ci) {
        if (ConfigRegistry.ELYTRA_REPLENISHER.getValue()) {
            ServerPlayer player = (ServerPlayer) (Object) this;

            // Check every 20 ticks
            if (player.tickCount % 10 != 0) return;

            ChunkPos currentChunk = player.chunkPosition();

            // Only check if player moved to a new chunk
            if (currentChunk.equals(packtools$lastCheckedChunk)) return;

            packtools$lastCheckedChunk = currentChunk;
            ElytraReplenisherHelper.checkPlayerInsideShip(player, currentChunk);
        }
    }
}