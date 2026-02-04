package com.frikinjay.packtools.client.gui;

import com.frikinjay.packtools.config.ConfigEntry;
import com.frikinjay.packtools.config.ConfigManager;
import com.frikinjay.packtools.config.ConfigRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PTConfigScreen extends Screen {
    private final Screen parent;
    private final List<ConfigRow> rows = new ArrayList<>();
    private Button saveButton;
    private int scrollOffset = 0;
    private static final int ROW_HEIGHT = 25;
    private static final int PADDING = 10;
    private static final int TOP_PADDING = 40;
    private static final int BOTTOM_PADDING = 36;

    // Scrollbar properties
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_PADDING = 2;
    private static final int SCROLLBAR_TOTAL_WIDTH = SCROLLBAR_WIDTH + SCROLLBAR_PADDING * 2;
    private boolean isDraggingScrollbar = false;
    private int scrollbarDragStartY = 0;
    private int scrollOffsetAtDragStart = 0;

    // Banner properties
    private static final Identifier BANNER_TEXTURE = Identifier.fromNamespaceAndPath("packtools", "textures/gui/packtools-banner.png");
    private static final Identifier BANNER_HOVER_TEXTURE = Identifier.fromNamespaceAndPath("packtools", "textures/gui/packtools-banner-hover.png");
    private static final int BANNER_WIDTH = 150;
    private static final int BANNER_HEIGHT = 24;

    // Button texture properties
    private static final Identifier BUTTON_TEXTURE = Identifier.fromNamespaceAndPath("packtools", "textures/gui/packtools-button.png");
    private static final Identifier BUTTON_HOVER_TEXTURE = Identifier.fromNamespaceAndPath("packtools", "textures/gui/packtools-button-hover.png");

    public PTConfigScreen(Screen parent) {
        super(Component.literal("PackTools Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        ConfigManager.reload();

        rows.clear();

        int yIndex = 0;
        String currentCategory = null;

        for (ConfigEntry<?> entry : ConfigRegistry.getAllEntries()) {
            if (!entry.getCategory().equals(currentCategory)) {
                currentCategory = entry.getCategory();
                rows.add(new CategoryRow(currentCategory, yIndex));
                yIndex++;
            }

            if (entry.getType() == Boolean.class) {
                rows.add(new BooleanRow((ConfigEntry<Boolean>) entry, yIndex));
            } else if (entry.getType() == Integer.class) {
                rows.add(new IntegerRow((ConfigEntry<Integer>) entry, yIndex));
            }

            yIndex++;
        }

        for (ConfigRow row : rows) {
            row.createWidget(this);
        }

        saveButton = this.addRenderableWidget(new CustomButton(
                this.width / 2 - 100,
                this.height - 28,
                200,
                20,
                Component.literal("Save & Exit"),
                button -> {
                    ConfigManager.save();
                    this.minecraft.setScreen(parent);
                },
                null
        ));
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderTransparentBackground(graphics);
        graphics.fill(0, 0, this.width, TOP_PADDING, 0x80000000);
        graphics.fill(0, this.height - BOTTOM_PADDING, this.width, this.height, 0x80000000);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        graphics.enableScissor(0, TOP_PADDING, this.width, this.height - BOTTOM_PADDING);

        for (ConfigRow row : rows) {
            int rowY = TOP_PADDING + (row.yIndex * ROW_HEIGHT) - scrollOffset;
            boolean visible = rowY >= TOP_PADDING - ROW_HEIGHT && rowY < this.height - BOTTOM_PADDING;
            row.updatePosition(rowY, visible);
        }

        // Render custom button backgrounds for boolean rows
        for (ConfigRow row : rows) {
            if (row instanceof BooleanRow) {
                int rowY = TOP_PADDING + (row.yIndex * ROW_HEIGHT) - scrollOffset;
                if (rowY >= TOP_PADDING - ROW_HEIGHT && rowY < this.height - BOTTOM_PADDING) {
                    // Custom buttons render their own backgrounds
                }
            }
        }

        for (var renderable : this.renderables) {
            if (renderable != saveButton) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
        }

        for (ConfigRow row : rows) {
            int rowY = TOP_PADDING + (row.yIndex * ROW_HEIGHT) - scrollOffset;
            if (rowY >= TOP_PADDING - ROW_HEIGHT && rowY < this.height - BOTTOM_PADDING) {
                row.renderLabel(graphics, rowY);
            }
        }

        graphics.disableScissor();

        // Render banner logo
        renderBanner(graphics, mouseX, mouseY);

        if (saveButton != null) {
            saveButton.render(graphics, mouseX, mouseY, partialTick);
        }

        renderScrollbar(graphics);
    }

    private void renderBanner(GuiGraphics graphics, int mouseX, int mouseY) {
        int bannerX = (this.width - BANNER_WIDTH) / 2;
        int bannerY = 8;

        boolean isHovered = mouseX >= bannerX && mouseX <= bannerX + BANNER_WIDTH &&
                mouseY >= bannerY && mouseY <= bannerY + BANNER_HEIGHT;

        Identifier texture = isHovered ? BANNER_HOVER_TEXTURE : BANNER_TEXTURE;

        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                bannerX,
                bannerY,
                0.0F,
                0.0F,
                BANNER_WIDTH,
                BANNER_HEIGHT,
                BANNER_WIDTH,
                BANNER_HEIGHT
        );
    }

    // Custom button class that renders our custom textures instead of vanilla backgrounds
    private static class CustomButton extends Button {
        private final String tooltipText;
        // Define the border sizes for 9-slice scaling (adjust these based on your texture design)
        private static final int BORDER_SIZE = 3; // Size of the corner/edge pieces

        public CustomButton(int x, int y, int width, int height, Component message, OnPress onPress, String tooltipText) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.tooltipText = tooltipText;
            if (tooltipText != null) {
                this.setTooltip(Tooltip.create(Component.literal(tooltipText)));
            }
        }

        @Override
        protected void renderContents(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Render custom background texture using 9-slice
            boolean isHovered = this.isHovered();
            Identifier texture = isHovered ? BUTTON_HOVER_TEXTURE : BUTTON_TEXTURE;

            renderNineSlice(graphics, texture, this.getX(), this.getY(), this.width, this.height);

            // Render the button text centered
            int conY = isHovered ? this.getY() + (this.height - 8) / 2 : (this.getY() + (this.height - 8) / 2) -1 ;
            int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    this.getMessage(),
                    this.getX() + this.width / 2,
                    conY,
                    textColor
            );
        }

        private void renderNineSlice(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height) {
            int textureWidth = 75; // Your source texture width
            int textureHeight = 20; // Your source texture height
            int border = BORDER_SIZE;

            // Corners (never stretched)
            // Top-left
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, border, border, textureWidth, textureHeight);
            // Top-right
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - border, y, textureWidth - border, 0.0F, border, border, textureWidth, textureHeight);
            // Bottom-left
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + height - border, 0.0F, textureHeight - border, border, border, textureWidth, textureHeight);
            // Bottom-right
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + width - border, y + height - border, textureWidth - border, textureHeight - border, border, border, textureWidth, textureHeight);

            // Edges (stretched in one direction)
            // Top edge
            renderRepeatingTexture(graphics, texture, x + border, y, width - border * 2, border, border, 0, textureWidth - border * 2, border, textureWidth, textureHeight);
            // Bottom edge
            renderRepeatingTexture(graphics, texture, x + border, y + height - border, width - border * 2, border, border, textureHeight - border, textureWidth - border * 2, border, textureWidth, textureHeight);
            // Left edge
            renderRepeatingTexture(graphics, texture, x, y + border, border, height - border * 2, 0, border, border, textureHeight - border * 2, textureWidth, textureHeight);
            // Right edge
            renderRepeatingTexture(graphics, texture, x + width - border, y + border, border, height - border * 2, textureWidth - border, border, border, textureHeight - border * 2, textureWidth, textureHeight);

            // Center (stretched in both directions)
            renderRepeatingTexture(graphics, texture, x + border, y + border, width - border * 2, height - border * 2, border, border, textureWidth - border * 2, textureHeight - border * 2, textureWidth, textureHeight);
        }

        private void renderRepeatingTexture(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
            int xOffset = 0;
            while (xOffset < width) {
                int yOffset = 0;
                int currentWidth = Math.min(regionWidth, width - xOffset);

                while (yOffset < height) {
                    int currentHeight = Math.min(regionHeight, height - yOffset);

                    graphics.blit(
                            RenderPipelines.GUI_TEXTURED,
                            texture,
                            x + xOffset,
                            y + yOffset,
                            u,
                            v,
                            currentWidth,
                            currentHeight,
                            textureWidth,
                            textureHeight
                    );

                    yOffset += currentHeight;
                }

                xOffset += currentWidth;
            }
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) {
            return;
        }

        int scrollbarX = this.width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
        int scrollbarTrackTop = 0;
        int scrollbarTrackBottom = this.height;
        int scrollbarTrackHeight = scrollbarTrackBottom - scrollbarTrackTop;

        graphics.fill(scrollbarX, scrollbarTrackTop, scrollbarX + SCROLLBAR_WIDTH, scrollbarTrackBottom, 0x40FFFFFF);

        int contentHeight = rows.size() * ROW_HEIGHT;
        int visibleHeight = this.height - BOTTOM_PADDING - TOP_PADDING;
        float scrollPercentage = (float) scrollOffset / maxScroll;

        int thumbHeight = Math.max(20, (int) ((float) visibleHeight / contentHeight * scrollbarTrackHeight));
        int thumbY = scrollbarTrackTop + (int) (scrollPercentage * (scrollbarTrackHeight - thumbHeight));

        graphics.fill(scrollbarX, thumbY, scrollbarX + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        scrollOffset -= (int) (deltaY * 20);
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScroll()));
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == 0) {
            int scrollbarX = this.width - SCROLLBAR_WIDTH - SCROLLBAR_PADDING;
            int scrollbarTrackTop = 0;
            int scrollbarTrackBottom = this.height;

            if (event.x() >= scrollbarX && event.x() <= scrollbarX + SCROLLBAR_WIDTH &&
                    event.y() >= scrollbarTrackTop && event.y() <= scrollbarTrackBottom) {

                isDraggingScrollbar = true;
                scrollbarDragStartY = (int) event.y();
                scrollOffsetAtDragStart = scrollOffset;
                return true;
            }
        }
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDraggingScrollbar = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double deltaX, double deltaY) {
        if (isDraggingScrollbar) {
            int scrollbarTrackTop = 0;
            int scrollbarTrackBottom = this.height;
            int scrollbarTrackHeight = scrollbarTrackBottom - scrollbarTrackTop;

            int contentHeight = rows.size() * ROW_HEIGHT;
            int visibleHeight = this.height - BOTTOM_PADDING - TOP_PADDING;
            int thumbHeight = Math.max(20, (int) ((float) visibleHeight / contentHeight * scrollbarTrackHeight));

            int mouseDelta = (int) event.y() - scrollbarDragStartY;
            int maxScroll = getMaxScroll();

            float scrollableTrackHeight = scrollbarTrackHeight - thumbHeight;
            scrollOffset = scrollOffsetAtDragStart + (int) ((mouseDelta / scrollableTrackHeight) * maxScroll);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    private int getMaxScroll() {
        int totalHeight = rows.size() * ROW_HEIGHT + TOP_PADDING;
        return Math.max(0, totalHeight - (this.height - BOTTOM_PADDING - TOP_PADDING));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private abstract static class ConfigRow {
        protected final int yIndex;

        public ConfigRow(int yIndex) {
            this.yIndex = yIndex;
        }

        public abstract void createWidget(Screen screen);
        public abstract void updatePosition(int y, boolean visible);
        public abstract void renderLabel(GuiGraphics graphics, int y);
    }

    private class CategoryRow extends ConfigRow {
        private final String name;

        public CategoryRow(String name, int yIndex) {
            super(yIndex);
            this.name = name;
        }

        @Override
        public void createWidget(Screen screen) {
        }

        @Override
        public void updatePosition(int y, boolean visible) {
        }

        @Override
        public void renderLabel(GuiGraphics graphics, int y) {
            graphics.drawString(font, "§n" + name, PADDING, y + 5, 0xFFFFFFFF);
        }
    }

    private class BooleanRow extends ConfigRow {
        private final ConfigEntry<Boolean> entry;
        private CustomButton button;

        public BooleanRow(ConfigEntry<Boolean> entry, int yIndex) {
            super(yIndex);
            this.entry = entry;
        }

        private Component getButtonText() {
            return Component.literal(entry.getValue() ? "§aON" : "§cOFF");
        }

        @Override
        public void createWidget(Screen screen) {
            int buttonX = width - 85 - SCROLLBAR_TOTAL_WIDTH;
            button = new CustomButton(
                    buttonX,
                    0,
                    75,
                    20,
                    getButtonText(),
                    btn -> {
                        entry.setValue(!entry.getValue());
                        btn.setMessage(getButtonText());
                    },
                    entry.getComment()
            );
            screen.addRenderableWidget(button);
        }

        @Override
        public void updatePosition(int y, boolean visible) {
            int buttonX = width - 85 - SCROLLBAR_TOTAL_WIDTH;
            button.setPosition(buttonX, y);
            button.visible = visible;
        }

        @Override
        public void renderLabel(GuiGraphics graphics, int y) {
            String name = formatName(entry.getName());
            graphics.drawString(font, name, PADDING + 10, y + 5, 0xFFFFFFFF);
        }
    }

    private class IntegerRow extends ConfigRow {
        private final ConfigEntry<Integer> entry;
        private EditBox textField;
        private boolean isUpdating = false;

        public IntegerRow(ConfigEntry<Integer> entry, int yIndex) {
            super(yIndex);
            this.entry = entry;
        }

        @Override
        public void createWidget(Screen screen) {
            int fieldX = width - 85 - SCROLLBAR_TOTAL_WIDTH;
            textField = new EditBox(font, fieldX, 0, 75, 20, Component.empty());
            textField.setValue(String.valueOf(entry.getValue()));
            textField.setFilter(text -> {
                return text.isEmpty() || text.equals("-") || text.matches("-?\\d+");
            });
            textField.setResponder(text -> {
                if (isUpdating) return;

                if (text.isEmpty() || text.equals("-")) {
                    return;
                }

                try {
                    int value = Integer.parseInt(text);
                    if (entry.hasRange()) {
                        int clamped = Math.max(entry.getMinValue(), Math.min(entry.getMaxValue(), value));
                        if (clamped != value) {
                            isUpdating = true;
                            textField.setValue(String.valueOf(clamped));
                            isUpdating = false;
                            value = clamped;
                        }
                    }
                    entry.setValue(value);
                } catch (NumberFormatException e) {
                    isUpdating = true;
                    textField.setValue(String.valueOf(entry.getValue()));
                    isUpdating = false;
                }
            });

            textField.setTooltip(Tooltip.create(Component.literal(entry.getComment())));
            screen.addRenderableWidget(textField);
        }

        @Override
        public void updatePosition(int y, boolean visible) {
            int fieldX = width - 85 - SCROLLBAR_TOTAL_WIDTH;
            textField.setPosition(fieldX, y);
            textField.visible = visible;

            if (!visible) {
                String text = textField.getValue();
                if (text.isEmpty() || text.equals("-")) {
                    isUpdating = true;
                    textField.setValue(String.valueOf(entry.getValue()));
                    isUpdating = false;
                }
            }
        }

        @Override
        public void renderLabel(GuiGraphics graphics, int y) {
            String name = formatName(entry.getName());
            String rangeInfo = entry.hasRange() ?
                    " (" + entry.getMinValue() + "-" + entry.getMaxValue() + ")" : "";
            graphics.drawString(font, name + rangeInfo, PADDING + 10, y + 5, 0xFFFFFFFF);
        }
    }

    private String formatName(String name) {
        if (name.endsWith("Enabled")) {
            name = name.substring(0, name.length() - 7);
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                result.append(' ');
            }
            result.append(c);
        }

        String formatted = result.toString();
        return formatted.substring(0, 1).toUpperCase() + formatted.substring(1);
    }
}