package com.frikinjay.packtools.features.packloader;

import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class PackToolsPackRepositorySource implements RepositorySource {

    @Override
    public void loadPacks(@NotNull Consumer<Pack> packConsumer) {
        if (PackLoaderConfig.DATAPACK_ORDERING != null) {
            PackLoaderHelper.initializeOrderingFile(PackLoaderConfig.DATAPACK_ORDERING);
        }
        if (PackLoaderConfig.RESOURCEPACK_ORDERING != null) {
            PackLoaderHelper.initializeOrderingFile(PackLoaderConfig.RESOURCEPACK_ORDERING);
        }

        if (PackLoaderConfig.LOAD_FROM_VANILLA_DATAPACKS) {
            PackLoaderHelper.loadPacks(
                    PackLoaderConfig.VANILLA_DATAPACK_DIR,
                    PackType.SERVER_DATA,
                    null,
                    packConsumer
            );
        }

        if (PackLoaderConfig.LOAD_FROM_VANILLA_RESOURCEPACKS) {
            PackLoaderHelper.loadPacks(
                    PackLoaderConfig.VANILLA_RESOURCEPACK_DIR,
                    PackType.CLIENT_RESOURCES,
                    null,
                    packConsumer
            );
        }

        PackLoaderHelper.loadPacks(
                PackLoaderConfig.DATAPACK_DIR,
                PackType.SERVER_DATA,
                PackLoaderConfig.DATAPACK_ORDERING,
                packConsumer
        );

        PackLoaderHelper.loadPacks(
                PackLoaderConfig.RESOURCEPACK_DIR,
                PackType.CLIENT_RESOURCES,
                PackLoaderConfig.RESOURCEPACK_ORDERING,
                packConsumer
        );
    }
}
