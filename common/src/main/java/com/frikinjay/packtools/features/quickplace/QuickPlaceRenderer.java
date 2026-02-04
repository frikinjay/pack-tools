package com.frikinjay.packtools.features.quickplace;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class QuickPlaceRenderer {
    private static final VoxelShape BLOCK_SHAPE = Shapes.block();

    private static final String VERTICAL_INDICATOR = "[  ]";
    private static final String HORIZONTAL_INDICATOR = "<  >";

    private static final RandomSource RANDOM = RandomSource.create();

    public static void render2D(GuiGraphics graphics, Minecraft mc) {
        QuickPlaceHelper.PlacementTarget target = QuickPlaceHelper.getCurrentTarget();
        if (target == null) {
            return;
        }

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int centerX = screenWidth >> 1;
        int centerY = screenHeight >> 1;

        String indicator = QuickPlaceHelper.isVertical() ? VERTICAL_INDICATOR : HORIZONTAL_INDICATOR;

        int ticks = QuickPlaceHelper.getDisplayTicks();
        float fade = Math.min(ticks * 0.2f, 1.0f);  // Pre-computed 1/5
        int alpha = (int) (fade * 255.0f);

        boolean canPlace = QuickPlaceHelper.canPlace(mc.level, mc.player);
        int baseColor = canPlace ? 0x00FFFFFF : 0x00FF5555;
        int color = (alpha << 24) | baseColor;

        int textWidth = font.width(indicator);
        int x = centerX - (textWidth >> 1);
        int y = centerY - 4;

        graphics.drawString(font, indicator, x, y, color, false);
    }

    public static void render3D(PoseStack poseStack, VertexConsumer buffer, Minecraft mc, float partialTick) {
        QuickPlaceHelper.PlacementTarget target = QuickPlaceHelper.getCurrentTarget();
        if (target == null) {
            return;
        }

        BlockPos pos = target.pos();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        Vec3 camPos = mc.gameRenderer.getMainCamera().position();

        boolean canPlace = QuickPlaceHelper.canPlace(level, mc.player);
        int color = canPlace ?
                ARGB.colorFromFloat(0.4f, 0.0f, 0.0f, 0.0f) :  // Black
                ARGB.colorFromFloat(0.4f, 1.0f, 0.3f, 0.3f);   // Red

        poseStack.pushPose();
        poseStack.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

        ShapeRenderer.renderShape(
                poseStack,
                buffer,
                BLOCK_SHAPE,
                0, 0, 0,
                color,
                mc.getWindow().getAppropriateLineWidth()
        );

        poseStack.popPose();
    }

    public static void renderTranslucentBlock(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            Minecraft mc,
            net.minecraft.client.Camera camera,
            float partialTick) {

        QuickPlaceHelper.PlacementTarget target = QuickPlaceHelper.getCurrentTarget();
        if (target == null) {
            return;
        }

        ItemStack stack = mc.player.getItemInHand(target.hand());
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return;
        }

        BlockPos pos = target.pos();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        Block block = blockItem.getBlock();
        BlockState stateToPlace = block.defaultBlockState();

        Vec3 camPos = camera.position();

        double offsetX = pos.getX() - camPos.x;
        double offsetY = pos.getY() - camPos.y;
        double offsetZ = pos.getZ() - camPos.z;

        poseStack.pushPose();
        poseStack.translate(offsetX, offsetY, offsetZ);

        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        ModelBlockRenderer modelRenderer = blockRenderer.getModelRenderer();

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderTypes.translucentMovingBlock());

        int packedLight = LevelRenderer.getLightColor(level, pos);

        boolean canPlace = QuickPlaceHelper.canPlace(level, mc.player);

        try {
            RANDOM.setSeed(42L);
            var model = blockRenderer.getBlockModel(stateToPlace);
            List<BlockModelPart> parts = model.collectParts(RANDOM);
            modelRenderer.tesselateBlock(
                    level,
                    parts,
                    stateToPlace,
                    pos,
                    poseStack,
                    vertexConsumer,
                    false,
                    packedLight
            );
        } catch (Exception e) {
        }

        poseStack.popPose();
    }
}