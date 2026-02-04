package com.frikinjay.packtools.features.doubledoors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class DoubleDoorsHelper {
    public static final int MAX_GATE_LIMIT = 64;

    public static BlockPos getMatchingDoor(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);

        Direction siblingDir = (hinge == DoorHingeSide.LEFT) ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos siblingPos = pos.relative(siblingDir);
        BlockState siblingState = level.getBlockState(siblingPos);

        if (siblingState.is(state.getBlock()) &&
                siblingState.getValue(DoorBlock.FACING) == facing &&
                siblingState.getValue(DoorBlock.HINGE) != hinge &&
                siblingState.getValue(DoorBlock.HALF) == state.getValue(DoorBlock.HALF)) {
            return siblingPos;
        }
        return null;
    }

    public static void updateFenceGates(Level level, BlockPos startPos, BlockState startState, boolean targetOpen) {
        Direction.Axis axis = startState.getValue(FenceGateBlock.FACING).getAxis();
        Block blockType = startState.getBlock();

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && visited.size() <= MAX_GATE_LIMIT) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);

            if (currentState.is(blockType) && currentState.getValue(BlockStateProperties.OPEN) != targetOpen) {
                level.setBlock(current, currentState.setValue(BlockStateProperties.OPEN, targetOpen), 10);
            }

            for (Direction dir : Direction.values()) {
                if (dir.getAxis() == Direction.Axis.Y || dir.getAxis() != axis) {
                    BlockPos next = current.relative(dir);
                    if (!visited.contains(next)) {
                        BlockState nextState = level.getBlockState(next);
                        if (nextState.is(blockType) && nextState.getValue(FenceGateBlock.FACING).getAxis() == axis) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
    }
}