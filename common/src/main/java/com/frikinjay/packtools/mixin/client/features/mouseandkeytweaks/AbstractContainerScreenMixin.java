package com.frikinjay.packtools.mixin.client.features.mouseandkeytweaks;

import com.frikinjay.packtools.client.features.mouseandkeytweaks.ClientMouseAndKeysTweaksHelper;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void packtools$onMouseClicked(MouseButtonEvent event, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (!ConfigRegistry.MOUSE_AND_KEY_TWEAKS.getValue()) return;

        ClientMouseAndKeysTweaksHelper.INSTANCE.onMousePressed(event.button());

        if (event.button() == 2) {
            // Sort inventory
            ClientMouseAndKeysTweaksHelper.INSTANCE.onMouseMiddleClicked(
                    (AbstractContainerScreen<?>) (Object) this,
                    this.hoveredSlot
            );
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"))
    private void packtools$onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigRegistry.MOUSE_AND_KEY_TWEAKS.getValue()) {
            ClientMouseAndKeysTweaksHelper.INSTANCE.onMouseReleased(event.button());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void packtools$onRender(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci) {
        if (ConfigRegistry.MOUSE_AND_KEY_TWEAKS.getValue()) {
            ClientMouseAndKeysTweaksHelper.INSTANCE.onRender(
                    (AbstractContainerScreen<?>) (Object) this,
                    this.hoveredSlot
            );
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void packtools$onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (ConfigRegistry.MOUSE_AND_KEY_TWEAKS.getValue()) {
            boolean handled = ClientMouseAndKeysTweaksHelper.INSTANCE.onMouseScrolled(
                    (AbstractContainerScreen<?>) (Object) this,
                    this.hoveredSlot,
                    scrollY
            );
            if (handled) {
                cir.setReturnValue(true);
            }
        }
    }
}