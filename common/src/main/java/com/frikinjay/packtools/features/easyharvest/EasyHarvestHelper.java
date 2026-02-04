package com.frikinjay.packtools.features.easyharvest;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.mixin.features.easyharvest.CropBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.NotNull;

public class EasyHarvestHelper {

    private static final Direction[] CARDINAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    public static final TagKey<@NotNull Block> TALL_HARVESTABLE = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "tall_harvestable")
    );

    public static final TagKey<@NotNull Block> HARVEST_BLACKLIST = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "harvest_blacklist")
    );

    public static boolean harvestCrop(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, boolean canRadiusHarvest) {
        if (player.isSpectator() || player.isCrouching()) return false;
        if (!isMatureCrop(state)) return false;
        ItemStack heldItem = player.getItemInHand(hand);
        if (canRadiusHarvest && heldItem.is(ItemTags.HOES)) {
            int radius = getHoeRadius(heldItem);
            if (radius > 0) {
                radiusHarvest(level, pos, player, hand, radius);
            }
        }
        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            Item seed = state.getBlock().asItem();
            boolean removedSeed = false;
            for (ItemStack drop : Block.getDrops(state, serverLevel, pos, null, player, heldItem)) {
                if (!removedSeed && drop.getItem() == seed && drop.getCount() > 0) {
                    drop.shrink(1);
                    removedSeed = true;
                }
                if (!drop.isEmpty()) {
                    Block.popResource(level, pos, drop);
                }
            }
            level.setBlockAndUpdate(pos, getResetState(state));
            if (heldItem.is(ItemTags.HOES)) {
                heldItem.hurtAndBreak(1, player,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            player.causeFoodExhaustion(0.005f);
        } else {
            player.playSound(state.getBlock() instanceof NetherWartBlock ?
                    SoundEvents.NETHER_WART_PLANTED : SoundEvents.CROP_PLANTED, 1.0f, 1.0f);
        }
        return true;
    }

    public static boolean harvestTallPlant(Level level, BlockPos clickPos, BlockState state, Player player, InteractionHand hand, Direction hitDirection) {
        if (player.isSpectator() || player.isCrouching()) return false;
        Block plantType = state.getBlock();
        ItemStack heldItem = player.getItemInHand(hand);
        Item plantItem = plantType.asItem();
        if (hitDirection == Direction.UP && heldItem.is(plantItem)) {
            return false;
        }
        BlockPos bottom = clickPos;
        while (level.getBlockState(bottom.below()).is(plantType)) {
            bottom = bottom.below();
        }
        if (!level.getBlockState(bottom.above()).is(plantType)) {
            return false;
        }
        BlockPos breakPos = bottom.above();
        if (!level.isClientSide()) {
            while (level.getBlockState(breakPos).is(plantType)) {
                Block.dropResources(state, level, breakPos, null, player, heldItem);
                level.removeBlock(breakPos, false);
                breakPos = breakPos.above();
            }
            player.causeFoodExhaustion(0.005f);
        } else {
            player.playSound(SoundEvents.CROP_PLANTED, 1.0f, 1.0f);
        }
        return true;
    }

    private static void radiusHarvest(Level level, BlockPos center, Player player, InteractionHand hand, int radius) {
        if (radius == 1) {
            for (Direction dir : CARDINAL) {
                BlockPos pos = center.relative(dir);
                BlockState state = level.getBlockState(pos);
                if (isMatureCrop(state)) {
                    harvestCrop(level, pos, state, player, hand, false);
                }
            }
        } else {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0) continue;

                    BlockPos pos = center.offset(x, 0, z);
                    BlockState state = level.getBlockState(pos);
                    if (isMatureCrop(state)) {
                        harvestCrop(level, pos, state, player, hand, false);
                    }
                }
            }
        }
    }

    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        return switch (block) {
            case CocoaBlock cocoaBlock -> state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
            case CropBlock crop -> crop.isMaxAge(state);
            case NetherWartBlock netherWartBlock -> state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
            default -> false;
        };

    }

    private static BlockState getResetState(BlockState state) {
        Block block = state.getBlock();
        switch (block) {
            case CocoaBlock cocoaBlock -> {
                return state.setValue(CocoaBlock.AGE, 0);
            }
            case CropBlock crop -> {
                IntegerProperty ageProperty = ((CropBlockAccessor) crop).invokeGetAgeProperty();
                return state.setValue(ageProperty, 0);
            }
            case NetherWartBlock netherWartBlock -> {
                return state.setValue(NetherWartBlock.AGE, 0);
            }
            default -> {
            }
        }
        return state;
    }

    private static int getHoeRadius(ItemStack hoe) {
        Item item = hoe.getItem();
        if (item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE) {
            return 2;
        }
        if (item == Items.WOODEN_HOE || item == Items.STONE_HOE ||
                item == Items.IRON_HOE || item == Items.GOLDEN_HOE) {
            return 1;
        }
        return 0;
    }
}