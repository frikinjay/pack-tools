package com.frikinjay.packtools;

import com.frikinjay.packtools.config.ConfigManager;
import com.frikinjay.packtools.registry.PTTags;
import com.frikinjay.packtools.util.tooltip.TooltipType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackTools
{
    public static final String MOD_ID = "packtools";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Internal
    public static boolean pinged = false;
    public static TooltipType[] tooltipOrder = new TooltipType[] {
            TooltipType.FOOD,     // 3rd
            TooltipType.EFFECTS,   // 2nd
            TooltipType.VANILLA  // 1st
    };

    public static void init() {
        //REGISTRY

        //Config
        //ConfigManager.init(CommonPlatformHelper.getGameDirectory());
        ConfigManager.init();

        //Tags
        PTTags.init();

        //DefaultOverrides
        //DefaultFilesManager.onGameStart(CommonPlatformHelper.getGameDirectory());

        //PackLoader
        //PackLoaderConfig.LOAD_FROM_VANILLA_DATAPACKS = false;
        //PackLoaderConfig.LOAD_FROM_VANILLA_RESOURCEPACKS = false;

        //QuickZoom
    }

}