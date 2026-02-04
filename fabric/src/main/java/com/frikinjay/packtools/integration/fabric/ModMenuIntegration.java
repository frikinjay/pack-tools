package com.frikinjay.packtools.integration.fabric;

import com.frikinjay.packtools.client.gui.PTConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PTConfigScreen::new;
    }
}