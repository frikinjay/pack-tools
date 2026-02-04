package com.frikinjay.packtools.features.packloader;

import com.frikinjay.packtools.PackTools;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Comparator;

public class PackLoaderHelper {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void loadPacks(Path packDir, PackType packType, Path orderingFile, java.util.function.Consumer<Pack> consumer) {
        if (!Files.exists(packDir)) {
            try {
                Files.createDirectories(packDir);
            } catch (IOException e) {
                PackTools.LOGGER.error("Failed to create pack directory: {}", packDir, e);
                return;
            }
        }

        List<Path> packPaths = findValidPacks(packDir);
        if (packPaths.isEmpty()) {
            return;
        }

        if (orderingFile != null && Files.exists(orderingFile)) {
            packPaths = applyOrdering(packPaths, orderingFile);
        }

        for (Path packPath : packPaths) {
            Pack pack = createPack(packPath, packType);
            if (pack != null) {
                consumer.accept(pack);
            }
        }
    }

    private static List<Path> findValidPacks(Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream.filter(PackLoaderHelper::isValidPack)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            PackTools.LOGGER.error("Failed to list packs in directory: {}", directory, e);
            return Collections.emptyList();
        }
    }

    private static boolean isValidPack(Path path) {
        if (Files.isRegularFile(path) && path.toString().endsWith(".zip")) {
            return true;
        }
        if (Files.isDirectory(path)) {
            return Files.exists(path.resolve("pack.mcmeta"));
        }
        return false;
    }

    private static List<Path> applyOrdering(List<Path> packs, Path orderingFile) {
        try {
            String json = Files.readString(orderingFile);
            PackOrdering ordering = GSON.fromJson(json, PackOrdering.class);

            if (ordering == null || ordering.loadOrder == null || ordering.loadOrder.length == 0) {
                return packs;
            }

            Map<String, Path> packMap = packs.stream()
                    .collect(Collectors.toMap(
                            p -> p.getFileName().toString(),
                            p -> p,
                            (a, b) -> a
                    ));

            List<Path> result = new ArrayList<>();
            Set<Path> orderedPackSet = new HashSet<>();

            List<Path> orderedPacks = new ArrayList<>();
            for (String packName : ordering.loadOrder) {
                Path packPath = packMap.get(packName);
                if (packPath != null) {
                    orderedPacks.add(packPath);
                    orderedPackSet.add(packPath);
                } else {
                    PackTools.LOGGER.warn("Pack '{}' specified in load order not found", packName);
                }
            }

            packs.stream()
                    .filter(p -> !orderedPackSet.contains(p))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(result::add);

            result.addAll(orderedPacks);

            return result;

        } catch (IOException | JsonSyntaxException e) {
            PackTools.LOGGER.error("Failed to read pack ordering file: {}", orderingFile, e);
            return packs;
        }
    }

    private static Pack createPack(Path packPath, PackType packType) {
        String packName = packPath.getFileName().toString();

        try {
            PackLocationInfo locationInfo = new PackLocationInfo(
                    packName,
                    Component.literal(packName),
                    PackSource.BUILT_IN,
                    Optional.empty()
            );

            PackSelectionConfig selectionConfig = new PackSelectionConfig(
                    true,
                    Pack.Position.TOP,
                    false
            );

            Pack.ResourcesSupplier resourcesSupplier = createResourcesSupplier(packPath);

            return Pack.readMetaAndCreate(locationInfo, resourcesSupplier, packType, selectionConfig);

        } catch (Exception e) {
            PackTools.LOGGER.error("Failed to create pack from: {}", packPath, e);
            return null;
        }
    }

    private static Pack.ResourcesSupplier createResourcesSupplier(Path path) {
        if (Files.isRegularFile(path) && path.toString().endsWith(".zip")) {
            return new FilePackResources.FileResourcesSupplier(path);
        } else if (Files.isDirectory(path)) {
            return new PathPackResources.PathResourcesSupplier(path);
        }
        throw new IllegalArgumentException("Invalid pack path: " + path);
    }

    public static void initializeOrderingFile(Path orderingFile) {
        if (Files.exists(orderingFile)) {
            return;
        }

        try {
            Files.createDirectories(orderingFile.getParent());
            PackOrdering emptyOrdering = new PackOrdering();
            String json = GSON.toJson(emptyOrdering);
            Files.writeString(orderingFile, json);
        } catch (IOException e) {
            PackTools.LOGGER.error("Failed to create ordering file: {}", orderingFile, e);
        }
    }

    private static class PackOrdering {
        public String[] loadOrder = new String[0];
    }
}