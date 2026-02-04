package com.frikinjay.packtools.mixin.client.features.mouseandkeytweaks;

import com.frikinjay.packtools.client.features.mouseandkeytweaks.ClientMouseAndKeysTweaksHelper;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void packtools$onRenderInventory(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (ConfigRegistry.MOUSE_AND_KEY_TWEAKS.getValue()) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
            Slot hoveredSlot = ((AbstractContainerScreenAccessor) screen).getHoveredSlot();
            ClientMouseAndKeysTweaksHelper.INSTANCE.onRender(screen, hoveredSlot);
        }
    }
}