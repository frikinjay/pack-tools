package com.frikinjay.packtools.mixin.features.packloader;

import com.frikinjay.packtools.features.packloader.PackToolsPackRepositorySource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(PackRepository.class)
public class PackRepositoryMixin {

    @Shadow
    @Final
    private Set<RepositorySource> sources;

    @Unique
    private boolean packtools$sourcesAdded = false;

    @Inject(method = "reload", at = @At("HEAD"))
    private void packtools$onReload(CallbackInfo ci) {
        if (!packtools$sourcesAdded) {
            sources.add(new PackToolsPackRepositorySource());
            packtools$sourcesAdded = true;
        }
    }
}