package com.frikinjay.packtools.config;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.platform.CommonPlatformHelper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;

    public static void init() {
        try {
            Path gameDir = CommonPlatformHelper.getGameDirectory();
            Path configDir = gameDir.resolve("config").resolve("packtools");

            Files.createDirectories(configDir);
            PackTools.LOGGER.info("[PackTools] Config directory: {}", configDir);

            configPath = configDir.resolve("packtools.json5");
            PackTools.LOGGER.info("[PackTools] Config path: {}", configPath);

            File file = configPath.toFile();
            if (file.exists()) {
                loadFromFile(file);
                PackTools.LOGGER.info("[PackTools] Loaded existing config");
            } else {
                PackTools.LOGGER.info("[PackTools] Creating new config with defaults");
            }

            // Always save to ensure file exists and is up to date
            save();
            PackTools.LOGGER.info("[PackTools] Config initialized");

        } catch (Exception e) {
            PackTools.LOGGER.error("[PackTools] Critical error in config initialization", e);
        }
    }

    private static void loadFromFile(File file) {
        try (FileReader reader = new FileReader(file)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            // Loop through all entries and load values
            for (ConfigEntry<?> entry : ConfigRegistry.getAllEntries()) {
                if (json.has(entry.getName())) {
                    try {
                        if (entry.getType() == Boolean.class) {
                            ((ConfigEntry<Boolean>) entry).setValue(json.get(entry.getName()).getAsBoolean());
                        } else if (entry.getType() == Integer.class) {
                            ((ConfigEntry<Integer>) entry).setValue(json.get(entry.getName()).getAsInt());
                        }
                    } catch (Exception e) {
                        PackTools.LOGGER.warn("[PackTools] Failed to load config entry: {}", entry.getName());
                    }
                }
            }

        } catch (Exception e) {
            PackTools.LOGGER.error("[PackTools] Failed to load config", e);
        }
    }

    public static void save() {
        if (configPath == null) {
            PackTools.LOGGER.error("[PackTools] Config path is null, cannot save");
            return;
        }

        try {
            Files.createDirectories(configPath.getParent());

            try (FileWriter writer = new FileWriter(configPath.toFile())) {
                writer.write("// PackTools Configuration\n");
                writer.write("// This file uses JSON5 syntax (https://json5.org/)\n");
                writer.write("{\n");

                ConfigEntry<?>[] entries = ConfigRegistry.getAllEntries().toArray(new ConfigEntry[0]);
                for (int i = 0; i < entries.length; i++) {
                    ConfigEntry<?> entry = entries[i];

                    // Write comment
                    writer.write("  // " + entry.getComment() + "\n");

                    // Write entry
                    writer.write("  \"" + entry.getName() + "\": ");

                    if (entry.getType() == Boolean.class || entry.getType() == Integer.class) {
                        writer.write(entry.getValue().toString());
                    } else {
                        writer.write(GSON.toJson(entry.getValue()));
                    }

                    if (i < entries.length - 1) {
                        writer.write(",");
                    }
                    writer.write("\n");

                    // Add blank line for readability
                    if (i < entries.length - 1) {
                        writer.write("\n");
                    }
                }

                writer.write("}\n");
            }

            PackTools.LOGGER.info("[PackTools] Saved config to {}", configPath);
        } catch (IOException e) {
            PackTools.LOGGER.error("[PackTools] Failed to save config", e);
        }
    }

    public static void reload() {
        File file = configPath.toFile();
        if (file.exists()) {
            loadFromFile(file);
            PackTools.LOGGER.info("[PackTools] Config reloaded");
        }
    }

    public static Path getConfigPath() {
        return configPath;
    }
}