package com.frikinjay.packtools.features.clumped;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.config.ConfigRegistry;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class ClumpedOrbHandler {

    private final ExperienceOrb orb;
    private Int2IntMap clumpedMap;
    private int cachedTotal = -1;
    private Optional<EnchantedItemInUse> currentRepairItem = Optional.empty();

    public ClumpedOrbHandler(ExperienceOrb orb) {
        this.orb = orb;
    }

    public Int2IntMap getClumpedMap() {
        if (clumpedMap == null) {
            clumpedMap = new Int2IntOpenHashMap();
            clumpedMap.put(orb.getValue(), 1);
        }
        return clumpedMap;
    }

    public void setClumpedMap(Int2IntMap map) {
        this.clumpedMap = map;
        this.cachedTotal = -1;
        resolve();
    }

    public boolean resolve() {
        Int2IntMap map = getClumpedMap();
        if (map.isEmpty()) {
            return orb.getValue() > 0;
        }

        if (cachedTotal < 0) {
            int total = 0;
            for (Int2IntMap.Entry entry : map.int2IntEntrySet()) {
                total += entry.getIntKey() * entry.getIntValue();
            }
            cachedTotal = total;
        }

        return cachedTotal > 0;
    }

    public int processPlayerPickup(ServerPlayer player, BiFunction<ServerPlayer, Integer, Integer> repairFunc) {
        Int2IntMap map = getClumpedMap();
        if (map.isEmpty()) {
            return 0;
        }

        int totalXp = 0;

        for (Int2IntMap.Entry entry : map.int2IntEntrySet()) {
            int xpValue = entry.getIntKey();
            int count = entry.getIntValue();

            for (int i = 0; i < count; i++) {
                int leftOver = repairFunc.apply(player, xpValue);
                totalXp += leftOver;
            }
        }

        return totalXp;
    }

    public void captureRepairItem(Optional<EnchantedItemInUse> item) {
        this.currentRepairItem = item;
    }

    public int calculateRepairAndRemainingXp(ServerPlayer player, int xpValue, BiFunction<ServerPlayer, Integer, Integer> fallbackRepair) {
        if (currentRepairItem.isEmpty()) {
            return xpValue;
        }

        ItemStack itemstack = currentRepairItem.get().itemStack();

        int xpToRepair = EnchantmentHelper.modifyDurabilityToRepairFromXp(
                player.level(),
                itemstack,
                xpValue * ConfigRegistry.MENDING_REPAIR_XP_RATIO.getValue()
        );

        int toRepair = Math.min(xpToRepair, itemstack.getDamageValue());
        itemstack.setDamageValue(itemstack.getDamageValue() - toRepair);

        if (toRepair > 0) {
            int xpUsed = toRepair * xpValue / xpToRepair;
            int remaining = xpValue - xpUsed;

            return remaining > 0 ? fallbackRepair.apply(player, remaining) : 0;
        }

        return xpValue;
    }

    public void mergeWith(ClumpedOrbHandler other, OrbAccessor thisAccessor, OrbAccessor otherAccessor) {
        Int2IntMap otherMap = other.getClumpedMap();
        Int2IntMap thisMap = getClumpedMap();

        for (Int2IntMap.Entry entry : otherMap.int2IntEntrySet()) {
            thisMap.mergeInt(entry.getIntKey(), entry.getIntValue(), Integer::sum);
        }

        thisAccessor.packtools$setCount(thisMap.values().intStream().sum());
        thisAccessor.packtools$setAge(Math.min(thisAccessor.packtools$getAge(), otherAccessor.packtools$getAge()));
        this.cachedTotal = -1;
    }

    public static boolean tryMergeToExisting(ServerLevel level, Vec3 pos, int value) {
        AABB aabb = AABB.ofSize(pos, 1.0, 1.0, 1.0);

        List<ExperienceOrb> list = level.getEntities(
                EntityTypeTest.forClass(ExperienceOrb.class),
                aabb,
                ExperienceOrb::isAlive
        );

        if (list.isEmpty()) {
            return false;
        }

        ExperienceOrb targetOrb = list.getFirst();
        ClumpedOrbHandler handler = ((IClumpedOrb) targetOrb).packtools$getHandler();
        Int2IntMap clumpedMap = handler.getClumpedMap();

        clumpedMap.mergeInt(value, 1, Integer::sum);

        OrbAccessor accessor = (OrbAccessor) targetOrb;
        accessor.packtools$setCount(clumpedMap.values().intStream().sum());
        accessor.packtools$setAge(0);
        handler.cachedTotal = -1;

        return true;
    }

    public void saveToNBT(ValueOutput nbt) {
        if (clumpedMap == null || clumpedMap.isEmpty()) {
            return;
        }

        int size = clumpedMap.size();
        int[] values = new int[size];
        int[] counts = new int[size];

        int index = 0;
        for (Int2IntMap.Entry entry : clumpedMap.int2IntEntrySet()) {
            values[index] = entry.getIntKey();
            counts[index] = entry.getIntValue();
            index++;
        }

        nbt.putIntArray("ClumpedXPValues", values);
        nbt.putIntArray("ClumpedXPCounts", counts);
    }

    public void loadFromNBT(ValueInput nbt, int defaultValue, int defaultCount) {
        Int2IntMap map = new Int2IntOpenHashMap();

        Optional<int[]> valuesOpt = nbt.getIntArray("ClumpedXPValues");
        Optional<int[]> countsOpt = nbt.getIntArray("ClumpedXPCounts");

        if (valuesOpt.isPresent() && countsOpt.isPresent()) {
            int[] values = valuesOpt.get();
            int[] counts = countsOpt.get();

            int length = Math.min(values.length, counts.length);
            for (int i = 0; i < length; i++) {
                if (counts[i] > 0) {
                    map.put(values[i], counts[i]);
                }
            }
        }
        setClumpedMap(map);
    }

    public interface OrbAccessor {
        int packtools$getAge();
        void packtools$setAge(int age);
        void packtools$setCount(int count);
    }
}