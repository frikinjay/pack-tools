package com.frikinjay.packtools.features.quickzoom;

import com.frikinjay.packtools.client.PackToolsClient;
import com.frikinjay.packtools.config.ConfigRegistry;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.util.Mth;

public final class QuickZoomHelper {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final double DEFAULT_ZOOM = 3.0;
    private static final double MIN_ZOOM = 1.0;
    private static final double MAX_ZOOM = 50.0;
    private static final double SCROLL_MULTIPLIER = 1.1;

    private static KeyMapping zoomKey;
    private static double zoomLevel = DEFAULT_ZOOM;
    private static boolean wasZooming = false;
    private static double savedSensitivity = -1.0;
    private static boolean savedCinematicCamera = false;

    public static KeyMapping createZoomKey() {
        if (zoomKey == null) {
            zoomKey = new KeyMapping(
                    "key.packtools.quickzoom",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V,
                    PackToolsClient.packToolsKeyCat
            );
        }
        return zoomKey;
    }

    public static float modifyFov(float originalFov) {
        if (!ConfigRegistry.QUICK_ZOOM.getValue() || zoomKey == null) {
            return originalFov;
        }

        boolean isZooming = zoomKey.isDown();

        // Handle zoom state changes
        if (isZooming != wasZooming) {
            handleZoomStateChange(isZooming);
            wasZooming = isZooming;
        }

        // Return modified FOV if zooming
        return isZooming ? (float)(originalFov / zoomLevel) : originalFov;
    }

    public static void handleScroll(double scrollAmount) {
        if (!ConfigRegistry.QUICK_ZOOM.getValue() || zoomKey == null || !zoomKey.isDown() || scrollAmount == 0) {
            return;
        }

        // Adjust zoom level
        zoomLevel *= (scrollAmount > 0) ? SCROLL_MULTIPLIER : (1.0 / SCROLL_MULTIPLIER);
        zoomLevel = Mth.clamp(zoomLevel, MIN_ZOOM, MAX_ZOOM);

        // Update sensitivity for new zoom level
        updateMouseSensitivity();
    }

    public static boolean shouldPreventHotbarScroll() {
        if (!ConfigRegistry.QUICK_ZOOM.getValue()) {
            return false;
        }
        return zoomKey != null && zoomKey.isDown();
    }

    private static void handleZoomStateChange(boolean isZooming) {
        if (isZooming) {
            // Starting zoom - save settings and apply zoom adjustments
            OptionInstance<@NotNull Double> sensitivity = MC.options.sensitivity();

            savedSensitivity = sensitivity.get();
            savedCinematicCamera = MC.options.smoothCamera;

            // Enable cinematic camera for smooth zooming
            MC.options.smoothCamera = true;

            updateMouseSensitivity();
        } else {
            // Ending zoom - restore original settings
            if (savedSensitivity >= 0) {
                MC.options.sensitivity().set(savedSensitivity);
                savedSensitivity = -1.0;
            }

            // Restore original cinematic camera setting
            MC.options.smoothCamera = savedCinematicCamera;
        }
    }

    private static void updateMouseSensitivity() {
        if (savedSensitivity < 0) {
            return;
        }

        // Scale sensitivity inversely with zoom level, but less aggressively
        // Using square root makes camera movement feel more natural at high zoom levels
        double adjustedSensitivity = savedSensitivity / Math.sqrt(zoomLevel-(zoomLevel*0.50));
        MC.options.sensitivity().set(adjustedSensitivity);
    }
}