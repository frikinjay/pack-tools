package com.frikinjay.packtools.features.packloader;

import com.frikinjay.packtools.platform.CommonPlatformHelper;

import java.nio.file.Path;

public class PackLoaderConfig {
    private static final Path GAME_DIR = CommonPlatformHelper.getGameDirectory();

    // Base configuration directory
    public static final Path CONFIG_DIR = GAME_DIR.resolve("config").resolve("packtools");

    // Pack directories
    public static Path DATAPACK_DIR = CONFIG_DIR.resolve("datapacks");
    public static Path RESOURCEPACK_DIR = CONFIG_DIR.resolve("resourcepacks");

    // Load from vanilla datapacks folder as well
    public static boolean LOAD_FROM_VANILLA_DATAPACKS = false;
    public static Path VANILLA_DATAPACK_DIR = GAME_DIR.resolve("datapacks");

    // Load from vanilla resourcepacks folder as well
    public static boolean LOAD_FROM_VANILLA_RESOURCEPACKS = false;
    public static Path VANILLA_RESOURCEPACK_DIR = GAME_DIR.resolve("resourcepacks");

    // Ordering files (null to disable ordering)
    public static Path DATAPACK_ORDERING = CONFIG_DIR.resolve("datapack_load_order.json");
    public static Path RESOURCEPACK_ORDERING = CONFIG_DIR.resolve("resourcepack_load_order.json");

    public static void setCustomPaths(Path datapackDir, Path resourcepackDir) {
        DATAPACK_DIR = datapackDir;
        RESOURCEPACK_DIR = resourcepackDir;
    }
}