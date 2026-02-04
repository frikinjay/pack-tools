package com.frikinjay.packtools.mixin.client.util.configbutton;

import com.frikinjay.packtools.client.util.ConfigButtonHelper;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void packtools$addCustomButton(CallbackInfo ci) {
        this.renderables.stream()
                .filter(w -> w instanceof Button b && b.getMessage().equals(Component.translatable("menu.singleplayer")))
                .findFirst()
                .ifPresent(widget -> {
                    AbstractWidget singleplayerBtn = (AbstractWidget) widget;

                    int x = singleplayerBtn.getX() + singleplayerBtn.getWidth() + 4;
                    int y = singleplayerBtn.getY() - singleplayerBtn.getHeight() - 4;

                    this.addRenderableWidget(ConfigButtonHelper.createConfigButton(x, y, this));
                });
    }
}