package com.frikinjay.packtools.fabric;

import com.frikinjay.packtools.features.defaultoverrides.DefaultFilesManager;
import com.frikinjay.packtools.platform.CommonPlatformHelper;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric PreLaunch - runs BEFORE any mod initialization
 * This is the earliest point in Fabric's lifecycle
 */
public class PackToolsPreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        // This runs before ANY mod initialization
        DefaultFilesManager.onGameStart(CommonPlatformHelper.getGameDirectory());
        DefaultFilesManager.LOGGER.info("Default files processed in pre-launch phase");
    }
}
