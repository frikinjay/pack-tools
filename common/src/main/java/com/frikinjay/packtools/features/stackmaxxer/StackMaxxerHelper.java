package com.frikinjay.packtools.features.stackmaxxer;

import net.minecraft.world.item.Item;

import static com.frikinjay.packtools.registry.PTTags.STACKMAXXER_BLACKLIST;

public class StackMaxxerHelper {

    public static int getModifiedStackSize(Item item, int currentMax) {
        if (currentMax >= 64 || item == null) {
            return currentMax;
        }
        if (item.getDefaultInstance().is(STACKMAXXER_BLACKLIST)) {
            return currentMax;
        }

        if (currentMax == 16) {
            return 64;
        } else if (currentMax == 1) {
            return 16;
        }

        return currentMax;
    }
}