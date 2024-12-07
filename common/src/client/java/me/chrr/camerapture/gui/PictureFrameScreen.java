package me.chrr.camerapture.gui;

import me.chrr.camerapture.Camerapture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.Consumer;

/// This UI is inspired largely by the equivalent UI in the
/// <a href="https://modrinth.com/mod/camera-mod">Forge camera mod</a>.
public class PictureFrameScreen extends HandledScreen<PictureFrameScreenHandler> implements ScreenHandlerListener {
    private static final Identifier TEXTURE = Camerapture.id("textures/gui/edit_picture_frame.png");

    private int frameWidth = 0;
    private int frameHeight = 0;
    private boolean glowing = false;
    private boolean fixed = false;

    private ButtonWidget upButton;
    private ButtonWidget leftButton;
    private ButtonWidget rightButton;
    private ButtonWidget downButton;

    private SmallCheckboxWidget glowingCheckbox;
    private SmallCheckboxWidget fixedCheckbox;

    public PictureFrameScreen(PictureFrameScreenHandler screenHandler, PlayerInventory inventory, Text title) {
        super(screenHandler, inventory, title);
        screenHandler.addListener(this);

        this.backgroundWidth = 158;
        this.backgroundHeight = 52;
    }

    @Override
    protected void init() {
        super.init();

        upButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button -> {
                            this.sendButtonPressPacket(hasShiftDown() ? 0 : 1);
                            this.frameHeight += hasShiftDown() ? -1 : 1;
                        })
                        .dimensions(width / 2 - backgroundWidth / 2, height / 2 - backgroundHeight / 2 - 20 - 4, backgroundWidth, 20)
                        .build());

        rightButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button -> {
                            this.sendButtonPressPacket(hasShiftDown() ? 2 : 3);
                            this.frameWidth += hasShiftDown() ? -1 : 1;
                        }).dimensions(width / 2 + backgroundWidth / 2 + 4, height / 2 - backgroundHeight / 2, 20, backgroundHeight)
                        .build());

        downButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button -> {
                            this.sendButtonPressPacket(hasShiftDown() ? 4 : 5);
                            this.frameHeight += hasShiftDown() ? -1 : 1;
                        })
                        .dimensions(width / 2 - backgroundWidth / 2, height / 2 + backgroundHeight / 2 + 4, backgroundWidth, 20)
                        .build());

        leftButton = addDrawableChild(
                ButtonWidget.builder(Text.empty(), button -> {
                            this.sendButtonPressPacket(hasShiftDown() ? 6 : 7);
                            this.frameWidth += hasShiftDown() ? -1 : 1;
                        }).dimensions(width / 2 - backgroundWidth / 2 - 20 - 4, height / 2 - backgroundHeight / 2, 20, backgroundHeight)
                        .build());

        glowingCheckbox = addDrawableChild(new SmallCheckboxWidget(Text.translatable("text.camerapture.edit_picture_frame.glowing"), (glowing) -> {
            this.sendButtonPressPacket(8);
            this.glowing = glowing;
        }, width / 2 - backgroundWidth / 2 + 7, height / 2 - backgroundHeight / 2 + 34, false, this.glowing));

        fixedCheckbox = addDrawableChild(new SmallCheckboxWidget(Text.translatable("text.camerapture.edit_picture_frame.fixed"), (fixed) -> {
            this.sendButtonPressPacket(9);
            this.fixed = fixed;
        }, width / 2 + backgroundWidth / 2 - 7 - 11, height / 2 - backgroundHeight / 2 + 34, true, this.fixed));

        updateButtons();
    }

    private void sendButtonPressPacket(int id) {
        if (this.client == null || this.client.interactionManager == null) {
            return;
        }

        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    @Override
    public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
        // This screen has no slots.
    }

    @Override
    public void onPropertyUpdate(ScreenHandler handler, int property, int value) {
        switch (property) {
            case 0 -> this.frameWidth = value;
            case 1 -> this.frameHeight = value;
            case 2 -> this.glowing = value == 1;
            case 3 -> this.fixed = value == 1;
        }

        updateButtons();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, x, y, 0f, 0f, backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.camerapture.edit_picture_frame.size", frameWidth, frameHeight), backgroundWidth / 2, 7, 0xffffff);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable("text.camerapture.edit_picture_frame.shrink_hint"), backgroundWidth / 2, 7 + textRenderer.fontHeight + 2, 0xa0a0a0);
    }

    private void updateButtons() {
        if (hasShiftDown()) {
            upButton.setMessage(Text.of("↓"));
            leftButton.setMessage(Text.of("→"));
            rightButton.setMessage(Text.of("←"));
            downButton.setMessage(Text.of("↑"));

            upButton.active = frameHeight > 1;
            leftButton.active = frameWidth > 1;
            rightButton.active = frameWidth > 1;
            downButton.active = frameHeight > 1;
        } else {
            upButton.setMessage(Text.of("↑"));
            leftButton.setMessage(Text.of("←"));
            rightButton.setMessage(Text.of("→"));
            downButton.setMessage(Text.of("↓"));

            upButton.active = frameHeight < 16;
            leftButton.active = frameWidth < 16;
            rightButton.active = frameWidth < 16;
            downButton.active = frameHeight < 16;
        }

        glowingCheckbox.checked = this.glowing;
        fixedCheckbox.checked = this.fixed;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        updateButtons();
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        updateButtons();
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        // We don't pause the game, so we can actually see the picture frame
        // update in the background while we're editing it.
        return false;
    }

    private static class SmallCheckboxWidget extends PressableWidget {
        private final boolean leftText;
        private boolean checked;

        private final Consumer<Boolean> onChange;

        public SmallCheckboxWidget(Text text, Consumer<Boolean> onChange, int x, int y, boolean leftText, boolean checked) {
            super(x, y, 11, 11, text);

            this.onChange = onChange;
            this.leftText = leftText;
            this.checked = checked;
        }

        @Override
        public void onPress() {
            this.checked = !this.checked;
            onChange.accept(checked);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, this.getNarrationMessage());

            if (this.active) {
                if (this.isFocused()) {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.focused"));
                } else {
                    builder.put(NarrationPart.USAGE, Text.translatable("narration.checkbox.usage.hovered"));
                }
            }
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            TextRenderer textRenderer = minecraftClient.textRenderer;

            int textX = getX() + (leftText ? -4 - textRenderer.getWidth(getMessage()) : 11 + 4);
            context.drawTextWithShadow(textRenderer, getMessage(), textX, getY() + 2, 0xe0e0e0);

            context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, getX(), getY(), this.isSelected() ? 11 : 0, 52, 11, 11, 256, 256);

            if (checked) {
                context.drawTexture(RenderLayer::getGuiTextured, TEXTURE, getX(), getY(), 22, 52, 11, 11, 256, 256);
            }
        }
    }
}