package com.frikinjay.packtools.client;

import com.frikinjay.packtools.PackTools;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackToolsClient {
    public static final KeyMapping.Category packToolsKeyCat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "features"));

    public static final Logger LOGGER = LoggerFactory.getLogger("packtools");

}
