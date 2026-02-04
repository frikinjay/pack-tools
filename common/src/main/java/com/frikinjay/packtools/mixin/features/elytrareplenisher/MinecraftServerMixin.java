package com.frikinjay.packtools.mixin.features.elytrareplenisher;

import com.frikinjay.packtools.config.ConfigRegistry;
import com.frikinjay.packtools.features.elytrareplenisher.ElytraReplenisherHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "loadLevel", at = @At("TAIL"))
    private void packtools$onStart(CallbackInfo ci) {
        if (ConfigRegistry.ELYTRA_REPLENISHER.getValue()) {
            MinecraftServer server = (MinecraftServer) (Object) this;
            File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
            ElytraReplenisherHelper.load(worldDir);
        }
    }

    @Inject(method = "saveEverything", at = @At("HEAD"))
    private void packtools$onSave(boolean bl, boolean bl2, boolean bl3, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigRegistry.ELYTRA_REPLENISHER.getValue()) {
            MinecraftServer server = (MinecraftServer) (Object) this;
            File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
            ElytraReplenisherHelper.save(worldDir);
        }
    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void packtools$onStop(CallbackInfo ci) {
        // Cleanup async executor on server stop
        if (ConfigRegistry.ELYTRA_REPLENISHER.getValue()) {
            ElytraReplenisherHelper.shutdown();
        }
    }
}