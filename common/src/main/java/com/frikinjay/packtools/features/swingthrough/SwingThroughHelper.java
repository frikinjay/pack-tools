package com.frikinjay.packtools.features.swingthrough;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import static com.frikinjay.packtools.registry.PTTags.SWINGTHROUGH_BLACKLIST;

public class SwingThroughHelper {
    public static final ThreadLocal<HitResult> STASHED_BLOCK_HIT = new ThreadLocal<>();

    public static boolean isSwingThroughBlock(HitResult hitResult) {
        return hitResult instanceof BlockHitResult blockHit && isTransparentBlock(blockHit.getBlockPos());
    }

    public static boolean isTransparentBlock(BlockPos pos) {
        if (!ConfigRegistry.SWING_THROUGH.getValue()) return false;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return false;
        BlockState state = level.getBlockState(pos);
        if (state.is(SWINGTHROUGH_BLACKLIST)) return false;
        return state.getCollisionShape(level, pos).isEmpty();
    }

    public static boolean isValidTarget(Entity target, Entity shooter, Vec3 startPos, double maxDistanceSqr) {
        if (startPos.distanceToSqr(target.position()) > maxDistanceSqr) return false;
        if (target.isSpectator() || target.equals(shooter.getVehicle())) return false;
        return target instanceof LivingEntity
                || target instanceof HangingEntity
                || target instanceof ArmorStand;
    }
}