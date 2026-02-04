package com.frikinjay.packtools.features.quickplace;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class QuickPlaceHelper {
    private static final double MIN_VERTICAL_DISTANCE = 1.0;
    private static final float LENIENCY = 0.5f;
    private static final float RANGE_ADJUSTMENT = 0.5f;

    private static PlacementTarget currentTarget = null;
    private static int displayTicks = 0;

    private static final Map<RaycastKey, HitResult> raycastCache = new HashMap<>();

    private static BlockState cachedClickedBlock = null;

    public record PlacementTarget(BlockPos pos, Direction dir, InteractionHand hand) {}

    public static void tick(Minecraft mc) {
        raycastCache.clear();
        cachedClickedBlock = null;
        currentTarget = null;

        if (mc.player == null || mc.level == null) {
            displayTicks = 0;
            return;
        }

        InteractionHand hand = getValidHand(mc.player);
        if (hand == null) {
            displayTicks = 0;
            return;
        }

        HitResult normalHit = performRaycast(mc.player, mc.level, 0, 0);

        if (normalHit.getType() == HitResult.Type.MISS) {
            currentTarget = findPlacementTarget(mc.player, mc.level, hand);
        }

        if (currentTarget != null) {
            displayTicks = Math.min(displayTicks + 1, 10);
        } else {
            displayTicks = 0;
        }
    }

    @Nullable
    private static InteractionHand getValidHand(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.getItem() instanceof BlockItem) {
            return InteractionHand.MAIN_HAND;
        }
        ItemStack offHand = player.getOffhandItem();
        if (offHand.getItem() instanceof BlockItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static HitResult performRaycast(LocalPlayer player, Level level, double yOffset, double backOffset) {
        Vec3 eyePos = player.getEyePosition(1.0f);
        Vec3 lookVec = player.getViewVector(1.0f);

        if (yOffset != 0) {
            eyePos = eyePos.add(0, yOffset, 0);
        }
        if (backOffset != 0) {
            Direction facing = player.getDirection();
            eyePos = eyePos.add(
                    -facing.getStepX() * backOffset,
                    0,
                    -facing.getStepZ() * backOffset
            );
        }

        double reach = player.blockInteractionRange() - RANGE_ADJUSTMENT;
        Vec3 endPos = eyePos.add(lookVec.scale(reach));

        RaycastKey key = new RaycastKey(eyePos, endPos);
        HitResult cached = raycastCache.get(key);
        if (cached != null) {
            return cached;
        }

        HitResult result = level.clip(new ClipContext(eyePos, endPos,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        raycastCache.put(key, result);
        return result;
    }

    @Nullable
    private static PlacementTarget findPlacementTarget(LocalPlayer player, Level level, InteractionHand hand) {
        PlacementTarget vertical = findVerticalTarget(player, level, hand);
        if (vertical != null) {
            return vertical;
        }
        return findHorizontalTarget(player, level, hand);
    }

    @Nullable
    private static PlacementTarget findVerticalTarget(LocalPlayer player, Level level, InteractionHand hand) {
        boolean lookingDown = player.getXRot() > 0;
        double yOffset = lookingDown ? LENIENCY : -LENIENCY;

        HitResult hit = performRaycast(player, level, yOffset, 0);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos targetPos = blockHit.getBlockPos().relative(lookingDown ? Direction.DOWN : Direction.UP);

            double distance = Math.abs(targetPos.getY() - player.getY());
            if (distance < MIN_VERTICAL_DISTANCE) {
                return null;
            }

            if (targetPos.getY() < level.getMinY() || targetPos.getY() >= level.getMaxY()) {
                return null;
            }

            BlockState state = level.getBlockState(targetPos);
            if (state.isAir() || state.canBeReplaced()) {
                return new PlacementTarget(targetPos, lookingDown ? Direction.DOWN : Direction.UP, hand);
            }
        }

        return null;
    }

    @Nullable
    private static PlacementTarget findHorizontalTarget(LocalPlayer player, Level level, InteractionHand hand) {
        Direction facing = player.getDirection();

        HitResult hit = performRaycast(player, level, 0, LENIENCY);

        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos targetPos = blockHit.getBlockPos().relative(facing);

            if (targetPos.getY() < level.getMinY() || targetPos.getY() >= level.getMaxY()) {
                return null;
            }

            BlockState state = level.getBlockState(targetPos);
            if (state.isAir() || state.canBeReplaced()) {
                return new PlacementTarget(targetPos, facing.getOpposite(), hand);
            }
        }

        return null;
    }

    private static boolean isTopSlab(BlockState state) {
        return state.hasProperty(BlockStateProperties.SLAB_TYPE) &&
                state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.TOP;
    }

    private static boolean isBottomSlab(BlockState state) {
        return state.hasProperty(BlockStateProperties.SLAB_TYPE) &&
                state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.BOTTOM;
    }

    @Nullable
    public static PlacementTarget getCurrentTarget() {
        return currentTarget;
    }

    public static int getDisplayTicks() {
        return displayTicks;
    }

    public static boolean hasActiveTarget() {
        return currentTarget != null;
    }

    public static boolean canPlace(Level level, LocalPlayer player) {
        if (currentTarget == null) return false;
        BlockState state = level.getBlockState(currentTarget.pos);
        return level.isUnobstructed(state, currentTarget.pos, net.minecraft.world.phys.shapes.CollisionContext.empty());
    }

    private static BlockState getClickedBlock(LocalPlayer player) {
        if (currentTarget == null) return null;

        if (cachedClickedBlock != null) {
            return cachedClickedBlock;
        }

        BlockPos clickedPos;
        if (isVertical()) {
            boolean lookingDown = player.getXRot() > 0;
            clickedPos = currentTarget.pos.relative(lookingDown ? Direction.UP : Direction.DOWN);
        } else {
            Direction facing = player.getDirection();
            clickedPos = currentTarget.pos.relative(facing.getOpposite());
        }

        cachedClickedBlock = player.level().getBlockState(clickedPos);
        return cachedClickedBlock;
    }

    @Nullable
    public static BlockHitResult createPlacementHit(LocalPlayer player) {
        if (currentTarget == null) return null;

        Vec3 hitVec = Vec3.atCenterOf(currentTarget.pos);

        boolean lookingDown = player.getXRot() > 0;
        if (isVertical() && lookingDown) {
            BlockState clickedBlock = getClickedBlock(player);
            if (clickedBlock != null && isTopSlab(clickedBlock)) {
                hitVec = hitVec.add(0, 1, 0);
            }
        }

        Direction direction;
        if (isVertical()) {
            direction = lookingDown ? Direction.UP : Direction.DOWN;
        } else {
            direction = player.getDirection().getOpposite();
        }

        return new BlockHitResult(hitVec, direction, currentTarget.pos, false);
    }

    public static boolean isVertical() {
        return currentTarget != null && currentTarget.dir.getAxis() == Direction.Axis.Y;
    }

    @Nullable
    public static InteractionHand getTargetHand() {
        return currentTarget != null ? currentTarget.hand : null;
    }

    public static boolean shouldAttemptQuickPlace(ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    private record RaycastKey(Vec3 start, Vec3 end) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof RaycastKey other)) return false;
            return Objects.equals(start, other.start) && Objects.equals(end, other.end);
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
}