package com.frikinjay.packtools.mixin.client.features.quickplace;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @Shadow @Final
    private Minecraft minecraft;

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void packtools$renderQuickPlaceOutline(
            GraphicsResourceAllocator graphicsResourceAllocator,
            DeltaTracker deltaTracker,
            boolean bl,
            Camera camera,
            Matrix4f matrix4f,
            Matrix4f matrix4f2,
            Matrix4f matrix4f3,
            GpuBufferSlice gpuBufferSlice,
            Vector4f vector4f,
            boolean bl2,
            CallbackInfo ci) {
/*
        if (!PackTools.quickPlaceEnabled) {
            return;
        }

        PoseStack poseStack = new PoseStack();

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);

        MultiBufferSource.BufferSource bufferSource = this.minecraft.renderBuffers().bufferSource();

        QuickPlaceRenderer.renderTranslucentBlock(
                poseStack,
                bufferSource,
                this.minecraft,
                camera,
                partialTick
        );

        bufferSource.endBatch();

        *//*QuickPlaceRenderer.render3D(
                poseStack,
                bufferSource.getBuffer(RenderTypes.lines()),
                this.minecraft,
                camera,
                partialTick
        );*//*

        bufferSource.endLastBatch();*/
    }
}