package com.frikinjay.packtools.neoforge;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.gui.PTConfigScreen;
import com.frikinjay.packtools.features.defaultoverrides.DefaultFilesManager;
import com.frikinjay.packtools.features.restocker.RestockerHelper;
import com.frikinjay.packtools.neoforge.network.PTNetworkingNeoForge;
import com.frikinjay.packtools.platform.CommonPlatformHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import com.frikinjay.packtools.platform.neoforge.CommonPlatformHelperImpl;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(PackTools.MOD_ID)
public final class PackToolsNeoForge {
    public PackToolsNeoForge(IEventBus modEventBus, ModContainer container) {
        PackTools.init();

        container.registerExtensionPoint(IConfigScreenFactory.class,
                (client, parent) -> new PTConfigScreen(parent));

        CommonPlatformHelperImpl.BLOCKS.register(modEventBus);
        CommonPlatformHelperImpl.ITEMS.register(modEventBus);

        modEventBus.addListener(this::onModConstruct);
        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::registerPackets);

        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerAboutToStartEvent(ServerAboutToStartEvent event) {
        //
    }

    @SubscribeEvent
    public void postServerStart(ServerStartedEvent event) {
        //
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // Rate limit cleanup when player disconnects
        RestockerHelper.removePlayerRateLimit(event.getEntity().getUUID());
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            //
        });
    }

    private void onModConstruct(FMLConstructModEvent event) {
        DefaultFilesManager.onGameStart(CommonPlatformHelper.getGameDirectory());
        DefaultFilesManager.LOGGER.info("Default files processed during mod construction");
    }

    private void registerPackets(RegisterPayloadHandlersEvent event) {
        PTNetworkingNeoForge.init(event);
    }
}