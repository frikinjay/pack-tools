package com.frikinjay.packtools.features.defaultoverrides;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.frikinjay.packtools.PackTools.MOD_ID;

/**
 * Manages default file replacements for game directory and world saves.
 * Files in packtools_defaults/ replace corresponding files in the game directory.
 * Files in packtools_defaults/default_saves/ replace files in all world save directories.
 * Only backs up and replaces files that have actually changed (uses hash comparison).
 * Maintains up to 5 timestamped backup folders by default.
 */
public class DefaultFilesManager {
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID+"_defaultOverrides");
    private static final String DEFAULTS_FOLDER = "packtools_defaults";
    private static final String BACKUP_FOLDER = "packtools_defaults_bak";
    private static final String DEFAULT_SAVES_FOLDER = "default_saves";
    private static final int MAX_BACKUPS = 5;
    private static final DateTimeFormatter BACKUP_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path gameDirectory;
    private final Path defaultsDirectory;
    private final Path backupRootDirectory;
    private final Path defaultSavesDirectory;
    private Path currentBackupDirectory;
    private boolean filesWereReplaced = false;
    private int filesSkipped = 0;
    private int filesReplaced = 0;

    private static DefaultFilesManager instance;

    public DefaultFilesManager(Path gameDirectory) {
        this.gameDirectory = gameDirectory;
        this.defaultsDirectory = gameDirectory.resolve(DEFAULTS_FOLDER);
        this.backupRootDirectory = gameDirectory.resolve(BACKUP_FOLDER);
        this.defaultSavesDirectory = defaultsDirectory.resolve(DEFAULT_SAVES_FOLDER);
    }

    public static void initialize(Path gameDirectory) {
        instance = new DefaultFilesManager(gameDirectory);
        instance.processDefaultFiles();
    }

/*
    public static DefaultFilesManager getInstance() {
        return instance;
    }
*/

    public void processDefaultFiles() {
        try {
            LOGGER.info("Starting default files processing...");

            createDirectories();

            filesSkipped = 0;
            filesReplaced = 0;

            processGlobalReplacements();

            if (filesWereReplaced) {
                cleanupOldBackups();
                LOGGER.info("Default files processing completed - {} files replaced, {} files skipped (no changes)",
                        filesReplaced, filesSkipped);
            } else {
                LOGGER.info("No files needed replacement - {} files already up to date", filesSkipped);
            }

        } catch (Exception e) {
            LOGGER.error("Error processing default files", e);
        }
    }

    public void processWorldDefaults(Path worldDirectory) {
        if (!Files.exists(defaultSavesDirectory)) {
            return;
        }

        try {
            LOGGER.info("Processing world defaults for: {}", worldDirectory.getFileName());
            //int worldFilesReplaced = 0;
            //int worldFilesSkipped = 0;

            Files.walkFileTree(defaultSavesDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path sourceFile, @NotNull BasicFileAttributes attrs) throws IOException {
                    Path relativePath = defaultSavesDirectory.relativize(sourceFile);
                    Path targetFile = worldDirectory.resolve(relativePath);

                    boolean replaced = backupAndReplace(targetFile, sourceFile, "world_" + worldDirectory.getFileName());

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                    if (!dir.equals(defaultSavesDirectory)) {
                        Path relativePath = defaultSavesDirectory.relativize(dir);
                        Path targetDir = worldDirectory.resolve(relativePath);
                        Files.createDirectories(targetDir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (Exception e) {
            LOGGER.error("Error processing world defaults for {}", worldDirectory, e);
        }
    }

    public void processAllWorldDefaults() {
        if (!Files.exists(defaultSavesDirectory)) {
            LOGGER.debug("No default_saves directory found, skipping world defaults");
            return;
        }

        Path savesDirectory = gameDirectory.resolve("saves");
        if (!Files.exists(savesDirectory)) {
            LOGGER.debug("No saves directory found");
            return;
        }

        try {
            Files.list(savesDirectory)
                    .filter(Files::isDirectory)
                    .forEach(this::processWorldDefaults);
        } catch (IOException e) {
            LOGGER.error("Error processing world defaults", e);
        }
    }

    private void createDirectories() throws IOException {
        Files.createDirectories(defaultsDirectory);
        Files.createDirectories(backupRootDirectory);
        LOGGER.debug("Created necessary directories");
    }

    private void ensureBackupFolderExists() throws IOException {
        if (currentBackupDirectory == null) {
            String timestamp = LocalDateTime.now().format(BACKUP_FOLDER_FORMAT);
            currentBackupDirectory = backupRootDirectory.resolve("backup_" + timestamp);
            Files.createDirectories(currentBackupDirectory);
            LOGGER.info("Created backup folder: {}", currentBackupDirectory.getFileName());
        }
    }

    private void cleanupOldBackups() {
        try {
            List<Path> backupFolders;
            try (Stream<Path> stream = Files.list(backupRootDirectory)) {
                backupFolders = stream
                        .filter(Files::isDirectory)
                        .filter(path -> path.getFileName().toString().startsWith("backup_"))
                        .sorted(Comparator.comparing(path -> {
                            try {
                                return Files.getLastModifiedTime((Path) path);
                            } catch (IOException e) {
                                return FileTime.fromMillis(0);
                            }
                        }).reversed())
                        .toList();
            }

            if (backupFolders.size() > MAX_BACKUPS) {
                LOGGER.info("Found {} backup folders, removing oldest {} to maintain limit of {}",
                        backupFolders.size(), backupFolders.size() - MAX_BACKUPS, MAX_BACKUPS);

                for (int i = MAX_BACKUPS; i < backupFolders.size(); i++) {
                    Path oldBackup = backupFolders.get(i);
                    deleteDirectory(oldBackup);
                    LOGGER.info("Deleted old backup: {}", oldBackup.getFileName());
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error cleaning up old backups", e);
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean filesAreIdentical(Path file1, Path file2) {
        try {

            boolean file1Exists = Files.exists(file1);
            boolean file2Exists = Files.exists(file2);

            if (!file1Exists || !file2Exists) {
                return false;
            }

            long size1 = Files.size(file1);
            long size2 = Files.size(file2);

            if (size1 != size2) {
                return false;
            }

            String hash1 = calculateFileHash(file1);
            String hash2 = calculateFileHash(file2);

            return hash1.equals(hash2);

        } catch (IOException e) {
            LOGGER.warn("Error comparing files {} and {}: {}", file1, file2, e.getMessage());
            return false;
        }
    }

    private String calculateFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream fis = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    private void processGlobalReplacements() throws IOException {
        if (!Files.exists(defaultsDirectory)) {
            LOGGER.warn("Defaults directory does not exist: {}", defaultsDirectory);
            return;
        }

        LOGGER.info("Processing global file replacements from: {}", defaultsDirectory);

        Files.walkFileTree(defaultsDirectory, new SimpleFileVisitor<Path>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path sourceFile, @NotNull BasicFileAttributes attrs) throws IOException {
                Path relativePath = defaultsDirectory.relativize(sourceFile);

                if (relativePath.startsWith(DEFAULT_SAVES_FOLDER)) {
                    return FileVisitResult.CONTINUE;
                }

                Path targetFile = gameDirectory.resolve(relativePath);

                backupAndReplace(targetFile, sourceFile, "global");

                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
                if (dir.equals(defaultSavesDirectory)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                if (!dir.equals(defaultsDirectory)) {
                    Path relativePath = defaultsDirectory.relativize(dir);
                    Path targetDir = gameDirectory.resolve(relativePath);
                    Files.createDirectories(targetDir);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean backupAndReplace(Path targetFile, Path sourceFile, String category) throws IOException {
        // Check if files are identical
        if (filesAreIdentical(targetFile, sourceFile)) {
            LOGGER.debug("Skipping {} - no changes detected", targetFile);
            filesSkipped++;
            return false;
        }

        if (Files.exists(targetFile)) {
            ensureBackupFolderExists();

            Path relativePath;
            if (category.startsWith("world_")) {
                String worldName = category.substring(6);
                relativePath = Paths.get(category, targetFile.getFileName().toString());

                Path worldDir = gameDirectory.resolve("saves").resolve(worldName);
                if (targetFile.startsWith(worldDir)) {
                    relativePath = Paths.get(category).resolve(worldDir.relativize(targetFile));
                }
            } else {
                relativePath = gameDirectory.relativize(targetFile);
            }

            Path backupFile = currentBackupDirectory.resolve(relativePath);
            Files.createDirectories(backupFile.getParent());

            Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.debug("Backed up: {} -> {}", targetFile, backupFile);
        } else {
            ensureBackupFolderExists();
        }

        Files.createDirectories(targetFile.getParent());

        Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Replaced: {} with {}", targetFile, sourceFile);

        filesWereReplaced = true;
        filesReplaced++;

        return true;
    }

    public static void onWorldLoad(MinecraftServer server) {
        if (instance != null && server != null) {
            Path worldDirectory = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            instance.processWorldDefaults(worldDirectory);
        }
    }

    public static void onGameStart(Path gameDirectory) {
        initialize(gameDirectory);

        if (instance != null) {
            instance.processAllWorldDefaults();
        }
    }
}