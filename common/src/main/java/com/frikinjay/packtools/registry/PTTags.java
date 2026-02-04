package com.frikinjay.packtools.registry;

import com.frikinjay.packtools.PackTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

public class PTTags {
    public static void init() {}

    public static final TagKey<@NotNull Item> ENCHANTABLE = TagKey.create(
            Registries.ITEM,
            registerTag("enchantable")
    );

    public static final TagKey<@NotNull Item> STACKMAXXER_BLACKLIST = TagKey.create(
            Registries.ITEM,
            registerTag("stackmaxxer_blacklist")
    );

    public static final TagKey<@NotNull Block> JUMPABLE = TagKey.create(
            Registries.BLOCK,
            registerTag("jumpable")
    );

    public static final TagKey<@NotNull Block> SWINGTHROUGH_BLACKLIST = TagKey.create(
            Registries.BLOCK,
            registerTag("swingthrough_blacklist")
    );

    public static final TagKey<@NotNull Item> RESTOCKER_BLACKLIST = TagKey.create(
            Registries.ITEM,
            registerTag("restocker_blacklist")
    );

    private static Identifier registerTag(String name) {
        return Identifier.fromNamespaceAndPath(PackTools.MOD_ID, name);
    }
}
