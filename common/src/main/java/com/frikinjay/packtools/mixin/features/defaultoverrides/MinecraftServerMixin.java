package com.frikinjay.packtools.mixin.features.defaultoverrides;

import com.frikinjay.packtools.features.defaultoverrides.DefaultFilesManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow
    public abstract Path getWorldPath(LevelResource levelResource);

    @Inject(method = "createLevels", at = @At("HEAD"))
    private void packtools$beforeWorldsLoad(CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;

        DefaultFilesManager.onWorldLoad(server);
        DefaultFilesManager.LOGGER.info("World defaults processed before world initialization");
    }
}