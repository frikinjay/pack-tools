package com.frikinjay.packtools.mixin.features.globalsounds;

import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(
            method = "globalLevelEvent",
            at = @At("HEAD"),
            cancellable = true
    )
    private void packTools$disableGlobalSounds(int eventId, BlockPos pos, int data, CallbackInfo ci) {
        if (ConfigRegistry.GLOBAL_SOUNDS_SUPPRESSOR.getValue()) {
            if (eventId == 1023 || eventId == 1028) {
                ServerLevel level = (ServerLevel) (Object) this;
                Vec3 soundPos = Vec3.atCenterOf(pos);
                double hearingRange = 128.0;
                for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
                    if (player.level() == level) {
                        double distanceSq = player.distanceToSqr(soundPos);
                        if (distanceSq < hearingRange * hearingRange) {
                            Vec3 packetPos;
                            if (distanceSq < (double) Mth.square(32)) {
                                packetPos = soundPos;
                            } else {
                                Vec3 direction = soundPos.subtract(player.position()).normalize();
                                packetPos = player.position().add(direction.scale(32.0));
                            }
                            player.connection.send(new ClientboundLevelEventPacket(
                                    eventId,
                                    BlockPos.containing(packetPos),
                                    data,
                                    true
                            ));
                        }
                    }
                }
                ci.cancel();
            }
        }
    }

}
