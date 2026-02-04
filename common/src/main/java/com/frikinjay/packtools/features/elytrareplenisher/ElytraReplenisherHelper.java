package com.frikinjay.packtools.features.elytrareplenisher;

import com.frikinjay.packtools.PackTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.structures.EndCityPieces;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ElytraReplenisherHelper {
    // Spatial indexing for fast ship lookup
    private static final Map<Long, List<ShipData>> SHIPS_BY_CHUNK = new ConcurrentHashMap<>();
    private static final List<ShipData> SHIPS = new CopyOnWriteArrayList<>();

    // Cache to avoid repeated structure lookups
    private static final Map<Long, Boolean> STRUCTURE_CHECK_CACHE = new ConcurrentHashMap<>();
    private static final int CACHE_CLEANUP_INTERVAL = 6000;
    private static final int MAX_CACHE_SIZE = 10000;

    // Thread pool for async operations - volatile and recreatable
    private static volatile ExecutorService ASYNC_EXECUTOR = null;

    // Pending async structure checks
    private static final Map<UUID, CompletableFuture<Void>> PENDING_CHECKS = new ConcurrentHashMap<>();

    private static final TagKey<@NotNull Structure> END_CITY_TAG = TagKey.create(
            Registries.STRUCTURE,
            Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "end_city")
    );

    public static class ShipData {
        public final BoundingBox box;

        public BlockPos framePos = BlockPos.ZERO;
        public String frameDir = "south";
        public Tag elytraItem;

        public BlockPos supportPos = BlockPos.ZERO;
        public String supportDir = "south";
        public String supportBlockId = "minecraft:purpur_block";

        public BlockPos headPos = BlockPos.ZERO;
        public String headDir = "north";

        public final Set<UUID> players = ConcurrentHashMap.newKeySet();

        // Track if ship needs saving
        public volatile boolean dirty = false;

        public ShipData(BoundingBox box) {
            this.box = box;
        }
    }

    /**
     * Gets or creates the executor service
     */
    private static synchronized ExecutorService getExecutor() {
        if (ASYNC_EXECUTOR == null || ASYNC_EXECUTOR.isShutdown()) {
            ASYNC_EXECUTOR = Executors.newFixedThreadPool(
                    2,
                    r -> {
                        Thread t = new Thread(r, "PT-ElytraReplenisher-Async");
                        t.setDaemon(true);
                        return t;
                    }
            );
        }
        return ASYNC_EXECUTOR;
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long)chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }

    private static void indexShip(ShipData ship) {
        int minChunkX = ship.box.minX() >> 4;
        int maxChunkX = ship.box.maxX() >> 4;
        int minChunkZ = ship.box.minZ() >> 4;
        int maxChunkZ = ship.box.maxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                SHIPS_BY_CHUNK.computeIfAbsent(
                        chunkKey(cx, cz),
                        k -> new CopyOnWriteArrayList<>()
                ).add(ship);
            }
        }
    }

    /**
     * Main check - called every 20 ticks when player changes chunks
     */
    public static void checkPlayerInsideShip(ServerPlayer player, ChunkPos currentChunk) {
        if (player.level().dimension() != Level.END) return;

        ServerLevel level = (ServerLevel) player.level();
        BlockPos pPos = player.blockPosition();

        // Periodic cache cleanup
        if (player.tickCount % CACHE_CLEANUP_INTERVAL == 0) {
            if (STRUCTURE_CHECK_CACHE.size() > MAX_CACHE_SIZE) {
                STRUCTURE_CHECK_CACHE.clear();
            }
        }

        // Check ships in player's chunk (fast, synchronous)
        long chunkKey = chunkKey(currentChunk.x, currentChunk.z);
        List<ShipData> nearbyShips = SHIPS_BY_CHUNK.get(chunkKey);

        if (nearbyShips != null) {
            for (ShipData ship : nearbyShips) {
                if (ship.box.isInside(pPos)) {
                    processReplenishment(player, ship);
                    return;
                }
            }
        }

        // Check cache for structure lookup result
        long posKey = pPos.asLong();
        Boolean cachedResult = STRUCTURE_CHECK_CACHE.get(posKey);

        if (cachedResult != null) {
            if (!cachedResult) return; // Already checked, no structure here
            // If cached as true, we need to check pieces again
        }

        // Do expensive structure lookup asynchronously
        checkStructureAsync(player, level, pPos, posKey);
    }

    /**
     * Async structure lookup to avoid blocking main thread
     */
    private static void checkStructureAsync(ServerPlayer player, ServerLevel level,
                                            BlockPos pPos, long posKey) {
        UUID playerId = player.getUUID();

        // Cancel any pending check for this player
        CompletableFuture<Void> existing = PENDING_CHECKS.get(playerId);
        if (existing != null && !existing.isDone()) {
            return; // Already checking
        }

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                // This is the expensive part - do it async
                StructureStart start = level.structureManager()
                        .getStructureWithPieceAt(pPos, END_CITY_TAG);

                boolean hasStructure = start != null && start.isValid();
                STRUCTURE_CHECK_CACHE.put(posKey, hasStructure);

                if (hasStructure) {
                    // Process on main thread (required for world modifications)
                    level.getServer().execute(() -> {
                        processStructureStart(player, start, pPos);
                    });
                }
            } catch (Exception e) {
                PackTools.LOGGER.error("Async structure check failed", e);
            } finally {
                PENDING_CHECKS.remove(playerId);
            }
        }, getExecutor());

        PENDING_CHECKS.put(playerId, future);
    }

    /**
     * Process structure start on main thread (must run on server thread)
     */
    private static void processStructureStart(ServerPlayer player, StructureStart start,
                                              BlockPos pPos) {
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof EndCityPieces.EndCityPiece cityPiece) {
                BoundingBox box = cityPiece.getBoundingBox();
                if (box.isInside(pPos) && box.getXSpan() > 12) {
                    registerNewShip(player, cityPiece);
                    return;
                }
            }
        }
    }

    /**
     * Flood fill search for dragon head starting from a known position (item frame)
     * Uses BFS to efficiently search the ship structure
     */
    private static BlockPos findDragonHeadFloodFill(ServerLevel level, BlockPos startPos,
                                                    BoundingBox box) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(startPos);
        visited.add(startPos);

        // BFS search with a reasonable limit to prevent infinite loops
        int maxSearched = 500;
        int searched = 0;

        while (!queue.isEmpty() && searched < maxSearched) {
            BlockPos current = queue.poll();
            searched++;

            // Check current position
            BlockState state = level.getBlockState(current);
            if (state.is(Blocks.DRAGON_HEAD) || state.is(Blocks.DRAGON_WALL_HEAD)) {
                return current;
            }

            // Add neighbors (6 directions)
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = current.relative(dir);

                // Only search within bounding box
                if (!box.isInside(neighbor)) continue;

                // Skip if already visited
                if (visited.contains(neighbor)) continue;

                visited.add(neighbor);

                // Add to queue if it's not air (part of structure)
                BlockState neighborState = level.getBlockState(neighbor);
                if (!neighborState.isAir()) {
                    queue.add(neighbor);
                }
            }
        }

        // Fallback: If flood fill doesn't find it (maybe disconnected),
        // do a quick scan of the entire bounding box as last resort
        return scanBoundingBoxForDragonHead(level, box);
    }

    /**
     * Fallback method: Scan entire bounding box for dragon head
     * Only called if flood fill fails
     */
    private static BlockPos scanBoundingBoxForDragonHead(ServerLevel level, BoundingBox box) {
        for (BlockPos pos : BlockPos.betweenClosed(
                box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ()
        )) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.DRAGON_HEAD) || state.is(Blocks.DRAGON_WALL_HEAD)) {
                return pos;
            }
        }
        return null;
    }

    private static void registerNewShip(ServerPlayer player, EndCityPieces.EndCityPiece piece) {
        ServerLevel level = (ServerLevel) player.level();
        BoundingBox box = piece.getBoundingBox();
        ShipData data = new ShipData(box);

        AABB area = new AABB(
                box.minX(), box.minY(), box.minZ(),
                box.maxX(), box.maxY(), box.maxZ()
        ).inflate(1.0);

        List<ItemFrame> frames = level.getEntitiesOfClass(ItemFrame.class, area);

        for (ItemFrame frame : frames) {
            if (frame.getItem().is(Items.ELYTRA)) {
                data.framePos = frame.blockPosition().immutable();
                data.frameDir = frame.getDirection().getName();

                RegistryOps<@NotNull Tag> ops = level.registryAccess()
                        .createSerializationContext(NbtOps.INSTANCE);
                data.elytraItem = ItemStack.CODEC
                        .encodeStart(ops, frame.getItem())
                        .result()
                        .orElse(null);

                data.supportPos = data.framePos
                        .relative(frame.getDirection().getOpposite())
                        .immutable();
                BlockState supportState = level.getBlockState(data.supportPos);
                data.supportBlockId = BuiltInRegistries.BLOCK
                        .getKey(supportState.getBlock())
                        .toString();

                if (supportState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                    data.supportDir = supportState
                            .getValue(HorizontalDirectionalBlock.FACING)
                            .getName();
                }
                break;
            }
        }

        // Flood fill search for dragon head starting from frame position
        BlockPos dragonHead = findDragonHeadFloodFill(level, data.framePos, box);
        if (dragonHead != null) {
            data.headPos = dragonHead.immutable();
            BlockState headState = level.getBlockState(dragonHead);
            if (headState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                data.headDir = headState.getValue(HorizontalDirectionalBlock.FACING).getName();
            }
        }

        SHIPS.add(data);
        indexShip(data);
        processReplenishment(player, data);
    }

    private static void processReplenishment(ServerPlayer player, ShipData ship) {
        UUID uuid = player.getUUID();
        if (!ship.players.contains(uuid)) {
            replenishTreasures((ServerLevel) player.level(), ship);
            ship.players.add(uuid);
            ship.dirty = true;
        }
    }

    private static void replenishTreasures(ServerLevel level, ShipData ship) {
        // 1. Restore Support Block
        if (level.getBlockState(ship.supportPos).isAir()) {
            Block block = BuiltInRegistries.BLOCK
                    .get(Identifier.parse(ship.supportBlockId))
                    .map(net.minecraft.core.Holder::value)
                    .orElse(Blocks.PURPUR_BLOCK);

            BlockState state = block.defaultBlockState();
            Direction sDir = Direction.byName(ship.supportDir);

            if (state.hasProperty(HorizontalDirectionalBlock.FACING) && sDir != null) {
                state = state.setValue(HorizontalDirectionalBlock.FACING, sDir);
            }
            level.setBlock(ship.supportPos, state, 3);
        }

        // 2. Restore Dragon Head (check for both types)
        BlockState currentHeadState = level.getBlockState(ship.headPos);
        if (!currentHeadState.is(Blocks.DRAGON_HEAD) && !currentHeadState.is(Blocks.DRAGON_WALL_HEAD)) {
            Direction hDir = Direction.byName(ship.headDir);
            level.destroyBlock(ship.headPos, true);
            level.setBlock(
                    ship.headPos,
                    Blocks.DRAGON_WALL_HEAD.defaultBlockState()
                            .setValue(
                                    HorizontalDirectionalBlock.FACING,
                                    hDir != null ? hDir : Direction.NORTH
                            ),
                    3
            );
        }

        // 3. Restore Item Frame
        AABB frameArea = new AABB(ship.framePos).inflate(0.1);
        List<ItemFrame> existingFrames = level.getEntitiesOfClass(
                ItemFrame.class,
                frameArea
        );
        ItemFrame frame = existingFrames.isEmpty() ? null : existingFrames.getFirst();

        if (frame == null) {
            Direction fDir = Direction.byName(ship.frameDir);
            frame = new ItemFrame(
                    level,
                    ship.framePos,
                    fDir != null ? fDir : Direction.SOUTH
            );
            level.destroyBlock(ship.framePos, true);
            level.addFreshEntity(frame);
        }

        if (frame.getItem().isEmpty()) {
            ItemStack stack = ItemStack.EMPTY;
            if (ship.elytraItem != null) {
                RegistryOps<@NotNull Tag> ops = level.registryAccess()
                        .createSerializationContext(NbtOps.INSTANCE);
                stack = ItemStack.CODEC
                        .parse(ops, ship.elytraItem)
                        .result()
                        .orElse(ItemStack.EMPTY);
            }
            frame.setItem(stack.isEmpty() ? new ItemStack(Items.ELYTRA) : stack);
        }
    }

    private static CompoundTag createStructuredPos(BlockPos pos, String dir, String id) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("dir", Objects.requireNonNullElse(dir, "south"));
        if (id != null) tag.putString("id", id);
        return tag;
    }

    /**
     * Save with GZIP compression for smaller file size
     */
    public static void save(File worldDir) {
        // Do save async to not block main thread
        CompletableFuture.runAsync(() -> {
            Path path = worldDir.toPath().resolve("data/packtools/elytra_ships.dat.gz");
            try {
                Files.createDirectories(path.getParent());
                CompoundTag root = new CompoundTag();
                ListTag shipList = new ListTag();

                for (ShipData ship : SHIPS) {
                    CompoundTag s = new CompoundTag();
                    s.putIntArray("box", new int[]{
                            ship.box.minX(), ship.box.minY(), ship.box.minZ(),
                            ship.box.maxX(), ship.box.maxY(), ship.box.maxZ()
                    });

                    s.put("frame_pos", createStructuredPos(ship.framePos, ship.frameDir, null));
                    if (ship.elytraItem != null) s.put("item", ship.elytraItem);

                    s.put("support_pos", createStructuredPos(
                            ship.supportPos,
                            ship.supportDir,
                            ship.supportBlockId
                    ));
                    s.put("head_pos", createStructuredPos(ship.headPos, ship.headDir, null));

                    ListTag pList = new ListTag();
                    for (UUID uuid : ship.players) {
                        pList.add(StringTag.valueOf(uuid.toString()));
                    }
                    s.put("uuids", pList);

                    shipList.add(s);
                    ship.dirty = false; // Clear dirty flag
                }

                root.put("ships", shipList);

                // Write with GZIP compression using DataOutputStream
                try (GZIPOutputStream gzipOut = new GZIPOutputStream(Files.newOutputStream(path));
                     DataOutputStream dataOut = new DataOutputStream(gzipOut)) {
                    NbtIo.write(root, dataOut);
                }

            } catch (IOException e) {
                PackTools.LOGGER.error("Failed to save elytra ships data", e);
            }
        }, getExecutor());
    }

    /**
     * Load with GZIP decompression
     */
    public static void load(File worldDir) {
        // Clear all data when loading a new world
        SHIPS.clear();
        SHIPS_BY_CHUNK.clear();
        STRUCTURE_CHECK_CACHE.clear();
        PENDING_CHECKS.clear();

        // Try compressed file first
        Path compressedPath = worldDir.toPath().resolve("data/packtools/elytra_ships.dat.gz");
        Path legacyPath = worldDir.toPath().resolve("data/packtools/elytra_ships.dat");

        Path path = Files.exists(compressedPath) ? compressedPath :
                (Files.exists(legacyPath) ? legacyPath : null);

        if (path == null) return;

        try {
            CompoundTag root;

            // Load based on file type
            if (path.toString().endsWith(".gz")) {
                try (GZIPInputStream gzipIn = new GZIPInputStream(Files.newInputStream(path));
                     DataInputStream dataIn = new DataInputStream(gzipIn)) {
                    root = NbtIo.read(dataIn);
                }
            } else {
                // Legacy uncompressed format
                root = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            }

            root.getList("ships").ifPresent(list -> {
                for (int i = 0; i < list.size(); i++) {
                    list.getCompound(i).ifPresent(tag -> {
                        tag.getIntArray("box").ifPresent(b -> {
                            ShipData data = new ShipData(
                                    new BoundingBox(b[0], b[1], b[2], b[3], b[4], b[5])
                            );

                            tag.getCompound("frame_pos").ifPresent(p -> {
                                data.framePos = new BlockPos(
                                        p.getIntOr("x", 0),
                                        p.getIntOr("y", 0),
                                        p.getIntOr("z", 0)
                                );
                                data.frameDir = p.getStringOr("dir", "south");
                            });
                            data.elytraItem = tag.get("item");

                            tag.getCompound("support_pos").ifPresent(p -> {
                                data.supportPos = new BlockPos(
                                        p.getIntOr("x", 0),
                                        p.getIntOr("y", 0),
                                        p.getIntOr("z", 0)
                                );
                                data.supportDir = p.getStringOr("dir", "south");
                                data.supportBlockId = p.getStringOr("id", "minecraft:oak_planks");
                            });

                            tag.getCompound("head_pos").ifPresent(p -> {
                                data.headPos = new BlockPos(
                                        p.getIntOr("x", 0),
                                        p.getIntOr("y", 0),
                                        p.getIntOr("z", 0)
                                );
                                data.headDir = p.getStringOr("dir", "north");
                            });

                            tag.getList("uuids").ifPresent(pList -> {
                                for (Tag value : pList) {
                                    value.asString().ifPresent(uuidStr -> {
                                        try {
                                            data.players.add(UUID.fromString(uuidStr));
                                        } catch (IllegalArgumentException e) {
                                            // Skip invalid UUIDs
                                        }
                                    });
                                }
                            });

                            SHIPS.add(data);
                            indexShip(data);
                        });
                    });
                }
            });
        } catch (IOException e) {
            PackTools.LOGGER.error("Failed to load elytra ships data", e);
        }
    }

    /**
     * Cleanup when server stops
     */
    public static void shutdown() {
        ExecutorService executor = ASYNC_EXECUTOR;
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}