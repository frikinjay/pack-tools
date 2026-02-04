package com.frikinjay.packtools.features.jumpover;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import static com.frikinjay.packtools.registry.PTTags.JUMPABLE;

public class JumpOverHelper {

    public static boolean nearJumpableBlock(LocalPlayer player) {
        BlockPos pos = player.blockPosition();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockState state = player.level().getBlockState(pos.offset(x, 0, z));
                if (state.is(JUMPABLE)) {
                    return true;
                }
            }
        }
        return false;
    }
}
