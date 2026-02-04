package com.frikinjay.packtools.client.util;

import com.frikinjay.packtools.PackTools;
import com.frikinjay.packtools.client.gui.PTConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class ConfigButtonHelper {

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "packtools-icon");
    private static final Identifier ICON_HOVER = Identifier.fromNamespaceAndPath(PackTools.MOD_ID, "packtools-icon-hover");

    public static Button createConfigButton(int x, int y, Screen parent) {
        Button.CreateNarration narration = (supplier) -> (MutableComponent) supplier.get();

        return new PTIconButton(
                x, y, 20, 20,
                Component.empty(),
                button -> Minecraft.getInstance().setScreen(new PTConfigScreen(parent)),
                narration
        );
    }

    public static class PTIconButton extends Button.Plain {
        public PTIconButton(int x, int y, int width, int height, Component message, OnPress onPress, CreateNarration narration) {
            super(x, y, width, height, message, onPress, narration);
        }

        @Override
        protected void renderContents(@NonNull GuiGraphics arg, int mouseX, int mouseY, float partialTick) {
            this.renderDefaultSprite(arg);

            Identifier currentIcon = this.isHovered() ? ICON_HOVER : ICON;

            arg.blitSprite(RenderPipelines.GUI_TEXTURED, currentIcon, this.getX() + 3, this.getY() + 3, 14, 14);
        }
    }
}