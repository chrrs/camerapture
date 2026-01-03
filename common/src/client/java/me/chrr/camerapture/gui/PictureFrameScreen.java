package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;
import net.minecraft.resources.Identifier;

import java.util.function.Consumer;

/// This UI is inspired largely by the equivalent UI in the
/// <a href="https://modrinth.com/mod/camera-mod">Forge camera mod</a>.
public class PictureFrameScreen extends AbstractContainerScreen<PictureFrameMenu> implements ContainerListener {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/edit_picture_frame.png");

    private int frameWidth = 0;
    private int frameHeight = 0;
    private boolean glowing = false;
    private boolean fixed = false;

    private Button upButton;
    private Button leftButton;
    private Button rightButton;
    private Button downButton;

    private SmallCheckboxWidget glowingCheckbox;
    private SmallCheckboxWidget fixedCheckbox;

    public PictureFrameScreen(PictureFrameMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        menu.addSlotListener(this);

        this.imageWidth = 158;
        this.imageHeight = 52;
    }

    @Override
    protected void init() {
        super.init();

        upButton = addRenderableWidget(
                Button.builder(Component.empty(), button -> {
                            this.sendButtonPressPacket(this.minecraft.hasShiftDown() ? 0 : 1);
                            this.frameHeight += this.minecraft.hasShiftDown() ? -1 : 1;
                        })
                        .bounds(width / 2 - imageWidth / 2, height / 2 - imageHeight / 2 - 20 - 4, imageWidth, 20)
                        .build());

        rightButton = addRenderableWidget(
                Button.builder(Component.empty(), button -> {
                            this.sendButtonPressPacket(this.minecraft.hasShiftDown() ? 2 : 3);
                            this.frameWidth += this.minecraft.hasShiftDown() ? -1 : 1;
                        }).bounds(width / 2 + imageWidth / 2 + 4, height / 2 - imageHeight / 2, 20, imageHeight)
                        .build());

        downButton = addRenderableWidget(
                Button.builder(Component.empty(), button -> {
                            this.sendButtonPressPacket(this.minecraft.hasShiftDown() ? 4 : 5);
                            this.frameHeight += this.minecraft.hasShiftDown() ? -1 : 1;
                        })
                        .bounds(width / 2 - imageWidth / 2, height / 2 + imageHeight / 2 + 4, imageWidth, 20)
                        .build());

        leftButton = addRenderableWidget(
                Button.builder(Component.empty(), button -> {
                            this.sendButtonPressPacket(this.minecraft.hasShiftDown() ? 6 : 7);
                            this.frameWidth += this.minecraft.hasShiftDown() ? -1 : 1;
                        }).bounds(width / 2 - imageWidth / 2 - 20 - 4, height / 2 - imageHeight / 2, 20, imageHeight)
                        .build());

        glowingCheckbox = addRenderableWidget(new SmallCheckboxWidget(Component.translatable("text.camerapture.edit_picture_frame.glowing"), (glowing) -> {
            this.sendButtonPressPacket(8);
            this.glowing = glowing;
        }, width / 2 - imageWidth / 2 + 7, height / 2 - imageHeight / 2 + 34, false, this.glowing));

        fixedCheckbox = addRenderableWidget(new SmallCheckboxWidget(Component.translatable("text.camerapture.edit_picture_frame.fixed"), (fixed) -> {
            this.sendButtonPressPacket(9);
            this.fixed = fixed;
        }, width / 2 + imageWidth / 2 - 7 - 11, height / 2 - imageHeight / 2 + 34, true, this.fixed));

        updateButtons();
    }

    private void sendButtonPressPacket(int id) {
        if (this.minecraft.gameMode == null) {
            return;
        }

        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    @Override
    public void slotChanged(AbstractContainerMenu menu, int slotId, ItemStack stack) {
        // This screen has no slots.
    }

    @Override
    public void dataChanged(AbstractContainerMenu menu, int property, int value) {
        switch (property) {
            case 0 -> this.frameWidth = value;
            case 1 -> this.frameHeight = value;
            case 2 -> this.glowing = value == 1;
            case 3 -> this.fixed = value == 1;
        }

        updateButtons();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, Component.translatable("text.camerapture.edit_picture_frame.size", frameWidth, frameHeight), imageWidth / 2, 7, CommonColors.WHITE);
        graphics.drawCenteredString(font, Component.translatable("text.camerapture.edit_picture_frame.shrink_hint"), imageWidth / 2, 7 + font.lineHeight + 2, CommonColors.GRAY);
    }

    private void updateButtons() {
        if (this.minecraft.hasShiftDown()) {
            upButton.setMessage(Component.nullToEmpty("↓"));
            leftButton.setMessage(Component.nullToEmpty("→"));
            rightButton.setMessage(Component.nullToEmpty("←"));
            downButton.setMessage(Component.nullToEmpty("↑"));

            upButton.active = frameHeight > 1;
            leftButton.active = frameWidth > 1;
            rightButton.active = frameWidth > 1;
            downButton.active = frameHeight > 1;
        } else {
            upButton.setMessage(Component.nullToEmpty("↑"));
            leftButton.setMessage(Component.nullToEmpty("←"));
            rightButton.setMessage(Component.nullToEmpty("→"));
            downButton.setMessage(Component.nullToEmpty("↓"));

            upButton.active = frameHeight < 16;
            leftButton.active = frameWidth < 16;
            rightButton.active = frameWidth < 16;
            downButton.active = frameHeight < 16;
        }

        glowingCheckbox.checked = this.glowing;
        fixedCheckbox.checked = this.fixed;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        updateButtons();
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        updateButtons();
        return super.keyReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        // We don't pause the game, so we can actually see the picture frame
        // update in the background while we're editing it.
        return false;
    }

    private static class SmallCheckboxWidget extends AbstractButton {
        private final boolean leftText;
        private boolean checked;

        private final Consumer<Boolean> onChange;

        public SmallCheckboxWidget(Component text, Consumer<Boolean> onChange, int x, int y, boolean leftText, boolean checked) {
            super(x, y, 11, 11, text);

            this.onChange = onChange;
            this.leftText = leftText;
            this.checked = checked;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            this.checked = !this.checked;
            onChange.accept(checked);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.createNarrationMessage());

            if (this.active) {
                String action = this.checked ? "uncheck" : "check";
                if (this.isFocused()) {
                    output.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.focused." + action));
                } else {
                    output.add(NarratedElementType.USAGE, Component.translatable("narration.checkbox.usage.hovered." + action));
                }
            }
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            Font font = Minecraft.getInstance().font;

            int textX = getX() + (leftText ? -4 - font.width(getMessage()) : 11 + 4);
            graphics.drawString(font, getMessage(), textX, getY() + 2, 0xffe0e0e0);

            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), this.isHoveredOrFocused() ? 11 : 0, 52, 11, 11, 256, 256);

            if (checked) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, getX(), getY(), 22, 52, 11, 11, 256, 256);
            }
        }
    }
}