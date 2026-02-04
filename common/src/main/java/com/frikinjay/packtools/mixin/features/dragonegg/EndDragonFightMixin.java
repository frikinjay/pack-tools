package com.frikinjay.packtools.mixin.features.dragonegg;

import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndDragonFight.class)
public class EndDragonFightMixin {

    @Inject(
            method = "setDragonKilled",
            at = @At("TAIL")
    )
    private void packTools$spawnDragonEgg(EnderDragon enderDragon, CallbackInfo ci) {
        if (ConfigRegistry.DRAGON_EGG.getValue()) {
            EndDragonFight endFight = ((EndDragonFight) (Object) this);
            if (enderDragon.getUUID().equals(endFight.getDragonUUID())) {
                if (endFight.hasPreviouslyKilledDragon()) {
                    endFight.level.setBlockAndUpdate(endFight.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, EndPodiumFeature.getLocation(endFight.origin)), Blocks.DRAGON_EGG.defaultBlockState());
                }
            }
        }
    }

}
