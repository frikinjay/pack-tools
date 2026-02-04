package com.frikinjay.packtools.mixin.client.features.hud;

import com.frikinjay.packtools.client.features.armorhud.ClientArmorHudHelper;
import com.frikinjay.packtools.client.features.effectshud.ClientEffectsHudHelper;
import com.frikinjay.packtools.client.features.healthhud.ClientHealthHudHelper;
import com.frikinjay.packtools.client.features.restorehud.ClientRestoreHudHelper;
import com.frikinjay.packtools.client.util.HudPositionManager;
import com.frikinjay.packtools.client.features.toughnesshud.ClientToughnessHudHelper;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private int tickCount;
    @Final @Shadow private RandomSource random;

    @Inject(method = "render", at = @At("TAIL"))
    private void packtools$renderCustomHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (!this.minecraft.options.hideGui && minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer()) {
            Player player = minecraft.player;
            int w = guiGraphics.guiWidth();
            int h = guiGraphics.guiHeight();

            // PRE-CALCULATE heights for custom HUD elements
            // This is done BEFORE layout calculation to avoid circular dependency
            int toughnessHeight = ClientToughnessHudHelper.getToughnessHudHeight(player);
            int effectsHeight = ClientEffectsHudHelper.getEffectsHudHeight(player);

            // CALCULATE layout once with all heights known
            // This is where vanilla element detection happens (air bubbles, vehicle health, etc.)
            HudPositionManager.HudLayout layout = HudPositionManager.calculateLayout(
                    w, h, player, toughnessHeight, effectsHeight
            );

            // RENDER all HUD elements using the calculated layout
            // Each helper uses layout.armorY, layout.toughnessY, layout.effectsBottomY
            if (ConfigRegistry.ARMOR_HUD.getValue()) {
                ClientArmorHudHelper.renderArmorHud(guiGraphics, layout, player);
            }

            if (ConfigRegistry.TOUGHNESS_HUD.getValue()) {
                ClientToughnessHudHelper.renderToughnessHud(guiGraphics, layout, player);
            }

            if (ConfigRegistry.EFFECTS_HUD.getValue()) {
                ClientEffectsHudHelper.renderEffectsHud(guiGraphics, layout, player);
            }
        }
    }

    // Modify the maxHealth variable after it's calculated but before it's used for row calculation
    @ModifyVariable(
            method = "renderPlayerHealth",
            at = @At(value = "STORE", ordinal = 0),
            ordinal = 0
    )
    private float packtools$limitMaxHealthForRendering(float maxHealth) {
        // Only cap if health HUD is enabled and maxHealth exceeds 20
        if (ConfigRegistry.HEALTH_HUD.getValue() && maxHealth > 20.0F) {
            return 20.0F;
        }
        return maxHealth;
    }

    @Inject(method = "renderHearts", at = @At("RETURN"))
    private void packtools$renderHealthOverlay(GuiGraphics guiGraphics, Player player, int leftX, int baseY, int rowSpacing, int regenHeartIndex, float maxHealth, int currentHealth, int displayHealth, int absorptionAmount, boolean blink, CallbackInfo ci) {
        if (ConfigRegistry.HEALTH_HUD.getValue()) {
            ClientHealthHudHelper.renderHealthOverlay(guiGraphics, player, leftX, baseY, currentHealth, displayHealth, blink);
        }

        // Render health restore overlay (shows predicted health gain from held consumable)
        if (ConfigRegistry.RESTORE_HUD.getValue()) {
            // Use system time for tick delta calculation
            float tickDelta = (System.currentTimeMillis() % 1000L) / 1000.0F;
            // Pass tickCount for jitter animation synchronization
            ClientRestoreHudHelper.renderHealthRestoreOverlay(guiGraphics, player, leftX, baseY, this.tickCount, this.random, tickDelta);
        }
    }

    @Inject(method = "renderFood", at = @At("RETURN"))
    private void packtools$renderFoodRestoreOverlay(GuiGraphics guiGraphics, Player player, int baseY, int rightX, CallbackInfo ci) {
        // Render food restore overlay (shows predicted food gain from held consumable)
        if (ConfigRegistry.RESTORE_HUD.getValue()) {
            float tickDelta = (System.currentTimeMillis() % 1000L) / 1000.0F;
            ClientRestoreHudHelper.renderFoodRestoreOverlay(guiGraphics, player, rightX, baseY, this.tickCount, this.random, tickDelta);
        }
    }

    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void packtools$cancelVanillaArmor(CallbackInfo ci) {
        if (ConfigRegistry.ARMOR_HUD.getValue()) {
            ci.cancel();
        }
    }
}