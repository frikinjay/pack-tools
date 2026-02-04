package com.frikinjay.packtools.platform;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.function.Supplier;

public class CommonPlatformHelper {

    @ExpectPlatform
    public static <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static boolean isModLoaded(String id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getModName(String id) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static Path getGameDirectory() {
        throw new AssertionError();
    }

}
