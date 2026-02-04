package com.frikinjay.packtools.platform.fabric;

import com.frikinjay.packtools.PackTools;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class CommonPlatformHelperImpl {
    public static List<Supplier<Block>> REGISTERED_BLOCKS = new ArrayList<>();

    public static <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        var registry = Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(PackTools.MOD_ID, name), block.get());
        REGISTERED_BLOCKS.add(() -> registry);
        return () -> registry;
    }

    public static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> item) {
        var registry = Registry.register(BuiltInRegistries.ITEM, Identifier.fromNamespaceAndPath(PackTools.MOD_ID, name), item.get());
        return () -> registry;
    }

    public static boolean isModLoaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    public static String getModName(String id) {
        if (FabricLoader.getInstance().getModContainer(id).isPresent()) {
            return FabricLoader.getInstance().getModContainer(id).get().getMetadata().getName();
        }
        return id;
    }

    public static Path getGameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }
}
